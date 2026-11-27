package com.premium.member.system.dto;

import java.time.OffsetDateTime;

import com.premium.member.system.entity.SysOperationLog;

/**
 * 操作日志响应。
 *
 * @param id        日志 ID
 * @param userId    操作人 ID
 * @param username  操作人登录名
 * @param module    模块
 * @param action    操作
 * @param method    HTTP 方法
 * @param path      请求路径
 * @param params    脱敏参数
 * @param ip        客户端 IP
 * @param userAgent User-Agent
 * @param success   是否成功
 * @param errorCode 错误码
 * @param costMs    耗时毫秒
 * @param createdAt 创建时间
 */
public record OperationLogResponse(
        Long id,
        Long userId,
        String username,
        String module,
        String action,
        String method,
        String path,
        String params,
        String ip,
        String userAgent,
        Integer success,
        String errorCode,
        Long costMs,
        OffsetDateTime createdAt) {

    public static OperationLogResponse from(SysOperationLog log) {
        return new OperationLogResponse(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getModule(),
                log.getAction(),
                log.getMethod(),
                log.getPath(),
                log.getParams(),
                log.getIp(),
                log.getUserAgent(),
                log.getSuccess(),
                log.getErrorCode(),
                log.getCostMs(),
                log.getCreatedAt());
    }
}
