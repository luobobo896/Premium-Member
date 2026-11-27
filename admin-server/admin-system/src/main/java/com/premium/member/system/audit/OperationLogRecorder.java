package com.premium.member.system.audit;

import java.time.OffsetDateTime;

import com.premium.member.framework.audit.AuditEvent;
import com.premium.member.framework.audit.AuditRecorder;
import com.premium.member.system.entity.SysOperationLog;
import com.premium.member.system.service.OperationLogService;
import org.springframework.stereotype.Component;

/**
 * 把切面之外的审计事件交给同一个异步写库服务。
 */
@Component
public class OperationLogRecorder implements AuditRecorder {

    private final OperationLogService operationLogService;

    public OperationLogRecorder(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public void recordFailure(AuditEvent event) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(event.userId());
        operationLog.setUsername(event.username());
        operationLog.setModule(event.module());
        operationLog.setAction(event.action());
        operationLog.setMethod(event.method());
        operationLog.setPath(event.path());
        operationLog.setParams(event.params());
        operationLog.setIp(event.ip());
        operationLog.setUserAgent(event.userAgent());
        operationLog.setSuccess(0);
        operationLog.setErrorCode(event.errorCode());
        operationLog.setCostMs(0L);
        operationLog.setCreatedAt(OffsetDateTime.now());
        operationLogService.record(operationLog);
    }
}
