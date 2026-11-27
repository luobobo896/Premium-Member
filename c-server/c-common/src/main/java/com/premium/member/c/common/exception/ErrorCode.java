package com.premium.member.c.common.exception;

/**
 * C 端业务错误码与对应 HTTP 状态。
 */
public enum ErrorCode {
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "参数校验失败"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或登录已过期"),
    TOKEN_INVALID(401, "TOKEN_INVALID", "Token 无效"),
    INVALID_CREDENTIALS(401, "INVALID_CREDENTIALS", "手机号或密码错误"),
    REFRESH_TOKEN_INVALID(401, "REFRESH_TOKEN_INVALID", "登录已过期，请重新登录"),
    FORBIDDEN(403, "FORBIDDEN", "没有权限"),
    USER_DISABLED(403, "USER_DISABLED", "账号已停用"),
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "未找到对应的会员信息"),
    CONFLICT(409, "CONFLICT", "资源冲突"),
    PHONE_ALREADY_REGISTERED(409, "PHONE_ALREADY_REGISTERED", "手机号已注册"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统繁忙，请稍后重试");

    private final int httpStatus;
    private final String code;
    private final String message;

    ErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
