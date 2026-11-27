package com.premium.member.common.exception;

/**
 * B 端业务错误码与 HTTP 状态。
 */
public enum ErrorCode {
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "参数校验失败"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或登录已过期"),
    TOKEN_INVALID(401, "TOKEN_INVALID", "Token 无效"),
    INVALID_CREDENTIALS(403, "INVALID_CREDENTIALS", "用户名或密码错误"),
    REFRESH_TOKEN_INVALID(403, "REFRESH_TOKEN_INVALID", "刷新令牌无效，请重新登录"),
    FORBIDDEN(403, "FORBIDDEN", "没有权限"),
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "会员不存在"),
    DEPT_NOT_FOUND(404, "DEPT_NOT_FOUND", "部门不存在"),
    CONFLICT(409, "CONFLICT", "资源冲突"),
    MEMBER_NO_EXISTS(409, "MEMBER_NO_EXISTS", "会员编号已存在"),
    USER_DISABLED(403, "USER_DISABLED", "账号已停用"),
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
