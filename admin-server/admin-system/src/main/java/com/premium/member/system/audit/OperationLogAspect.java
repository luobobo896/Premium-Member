package com.premium.member.system.audit;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.framework.audit.AuditContext;
import com.premium.member.framework.security.AuthenticatedUser;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.entity.SysOperationLog;
import com.premium.member.system.service.OperationLogService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 记录增删改与登录成败，参数脱敏后异步落库。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_PARAMS = 2000;
    private static final int MAX_USER_AGENT = 512;

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogService operationLogService, ObjectMapper objectMapper) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        AuditContext.clear();
        long start = System.currentTimeMillis();
        Throwable failure = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            try {
                write(joinPoint, auditLog, System.currentTimeMillis() - start, failure);
            } finally {
                AuditContext.clear();
            }
        }
    }

    private void write(ProceedingJoinPoint joinPoint, AuditLog auditLog, long costMs, Throwable failure) {
        try {
            String params = buildParams(joinPoint);
            AuthenticatedUser user = currentUser();
            SysOperationLog operationLog = new SysOperationLog();
            Long userId = AuditContext.userId();
            if (user != null) {
                userId = user.userId();
            }
            operationLog.setUserId(userId);
            operationLog.setUsername(user == null ? paramUsername(params) : user.username());
            operationLog.setModule(auditLog.module());
            operationLog.setAction(auditLog.action());
            operationLog.setParams(params);
            operationLog.setSuccess(failure == null ? 1 : 0);
            String contextError = AuditContext.errorCode();
            operationLog.setErrorCode(contextError == null ? errorCode(failure) : contextError);
            operationLog.setCostMs(costMs);
            operationLog.setCreatedAt(OffsetDateTime.now());
            fillRequest(operationLog);
            operationLogService.record(operationLog);
        } catch (Exception ex) {
            log.warn("Operation log interception failed: {} {}", auditLog.module(), auditLog.action(), ex);
        }
    }

    private void fillRequest(SysOperationLog operationLog) {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        operationLog.setMethod(request.getMethod());
        operationLog.setPath(request.getRequestURI());
        operationLog.setIp(clientIp(request));
        operationLog.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT));
    }

    private String buildParams(ProceedingJoinPoint joinPoint) throws Exception {
        ArrayNode array = objectMapper.createArrayNode();
        for (Object argument : joinPoint.getArgs()) {
            if (skip(argument)) {
                continue;
            }
            JsonNode node = objectMapper.valueToTree(argument);
            sanitize(node);
            array.add(node);
        }
        return truncate(objectMapper.writeValueAsString(array), MAX_PARAMS);
    }

    private boolean skip(Object argument) {
        return argument == null
                || argument instanceof ServletRequest
                || argument instanceof ServletResponse
                || argument instanceof BindingResult
                || argument instanceof MultipartFile
                || argument instanceof MultipartFile[];
    }

    private void sanitize(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> fields = new ArrayList<>();
            node.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                if (sensitive(field)) {
                    objectNode.put(field, "***");
                } else {
                    sanitize(objectNode.get(field));
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::sanitize);
        }
    }

    private boolean sensitive(String field) {
        String lower = field.toLowerCase();
        return lower.contains("password")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("authorization");
    }

    private String paramUsername(String params) {
        try {
            JsonNode root = objectMapper.readTree(params);
            JsonNode first = root.isArray() && !root.isEmpty() ? root.get(0) : null;
            JsonNode username = first == null ? null : first.get("username");
            return username == null || username.isNull() ? "" : truncate(username.asText(), 64);
        } catch (Exception ex) {
            return "";
        }
    }

    private AuthenticatedUser currentUser() {
        try {
            return SecurityUtils.currentUser();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String errorCode(Throwable failure) {
        if (failure == null) {
            return null;
        }
        if (failure instanceof BusinessException businessException) {
            return businessException.errorCode().code();
        }
        if (failure instanceof AccessDeniedException) {
            return "FORBIDDEN";
        }
        return "INTERNAL_ERROR";
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        String realIp = request.getHeader("X-Real-IP");
        return truncate(realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp, 64);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
