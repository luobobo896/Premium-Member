package com.premium.member.framework.audit;

/**
 * 无法进入切面的审计事件，目前用于参数校验失败。
 */
public record AuditEvent(
        Long userId,
        String username,
        String module,
        String action,
        String method,
        String path,
        String params,
        String ip,
        String userAgent,
        String errorCode) {
}
