package com.premium.member.framework.web;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.premium.member.common.api.R;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.audit.AuditEvent;
import com.premium.member.framework.audit.AuditRecorder;
import com.premium.member.framework.security.AuthenticatedUser;
import com.premium.member.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 统一异常出口：4xx 返回业务错误体，5xx 隐藏内部细节并记录日志。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int MAX_SUMMARY = 2000;
    private static final int MAX_USER_AGENT = 512;
    private static final Pattern SECRET_TEXT = Pattern.compile(
            "(?i)(password|token|secret|authorization)([\\s\"']*[:=][\\s\"']*)([^,;\\s}\"']+)");

    private final ObjectProvider<AuditRecorder> auditRecorder;

    public GlobalExceptionHandler(ObjectProvider<AuditRecorder> auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.errorCode();
        log.warn("Business exception: bizCode={}, message={}", code.code(), ex.getMessage());
        return ResponseEntity.status(code.httpStatus()).body(R.fail(code.code(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        auditValidation(ex, fieldSummary(ex.getBindingResult().getFieldErrors()));
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.message());
        return validationError(message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException ex) {
        auditValidation(ex, fieldSummary(ex.getFieldErrors()));
        String message = ex.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.message());
        return validationError(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String summary = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    return sensitive(path) ? path + ": ***" : path + ": " + violation.getMessage();
                })
                .collect(Collectors.joining("; "));
        auditValidation(ex, summary);
        return validationError(ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<R<Void>> handleBadRequest(Exception ex) {
        auditValidation(ex, "请求参数无法解析");
        return validationError(ErrorCode.VALIDATION_ERROR.message());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.httpStatus())
                .body(R.fail(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.httpStatus())
                .body(R.fail(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.httpStatus())
                .body(R.fail(ErrorCode.NOT_FOUND.code(), ErrorCode.NOT_FOUND.message()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(R.fail("METHOD_NOT_ALLOWED", "请求方法不支持"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(R.fail(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message()));
    }

    private ResponseEntity<R<Void>> validationError(String message) {
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(R.fail(ErrorCode.VALIDATION_ERROR.code(), message));
    }

    private String fieldSummary(List<FieldError> errors) {
        if (errors.isEmpty()) {
            return ErrorCode.VALIDATION_ERROR.message();
        }
        return errors.stream()
                .map(error -> sensitive(error.getField())
                        ? error.getField() + ": ***"
                        : error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private void auditValidation(Exception ex, String summary) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String path = request.getRequestURI();
            boolean login = path != null && path.endsWith("/auth/login");
            AuditEvent event = new AuditEvent(
                    currentUserId(),
                    attemptedUsername(ex),
                    login ? "认证" : "请求",
                    login ? "登录" : "参数校验",
                    request.getMethod(),
                    truncate(path, 255),
                    truncate(sanitize(summary), MAX_SUMMARY),
                    clientIp(request),
                    truncate(request.getHeader("User-Agent"), MAX_USER_AGENT),
                    ErrorCode.VALIDATION_ERROR.code());
            AuditRecorder recorder = auditRecorder.getIfAvailable();
            if (recorder != null) {
                recorder.recordFailure(event);
            }
        } catch (RuntimeException auditFailure) {
            log.warn("Validation audit failed", auditFailure);
        }
    }

    private String attemptedUsername(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException invalid) {
            return fieldValue(invalid.getBindingResult(), "username");
        }
        if (ex instanceof BindException bind) {
            return fieldValue(bind.getBindingResult(), "username");
        }
        if (ex instanceof ConstraintViolationException violations) {
            return violations.getConstraintViolations().stream()
                    .filter(violation -> violation.getPropertyPath().toString().endsWith("username"))
                    .findFirst()
                    .map(violation -> String.valueOf(violation.getInvalidValue()))
                    .orElse("");
        }
        return "";
    }

    private String fieldValue(BindingResult result, String field) {
        Object value = result.getFieldValue(field);
        return value == null ? "" : truncate(String.valueOf(value), 64);
    }

    private Long currentUserId() {
        try {
            AuthenticatedUser user = SecurityUtils.currentUser();
            return user.userId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean sensitive(String field) {
        String lower = field == null ? "" : field.toLowerCase();
        return lower.contains("password")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("authorization");
    }

    private String sanitize(String text) {
        return text == null ? "" : SECRET_TEXT.matcher(text).replaceAll("$1$2***");
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
