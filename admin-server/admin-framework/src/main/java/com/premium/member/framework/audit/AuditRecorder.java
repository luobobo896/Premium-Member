package com.premium.member.framework.audit;

/**
 * 审计写入口，避免 framework 反向依赖 system 模块。
 */
public interface AuditRecorder {

    void recordFailure(AuditEvent event);
}
