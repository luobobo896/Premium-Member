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
    MEMBER_DISABLED(403, "MEMBER_DISABLED", "会员状态已停用"),
    BENEFIT_PERIOD_NOT_FOUND(404, "BENEFIT_PERIOD_NOT_FOUND", "当前没有生效中的权益周期"),
    BENEFIT_PRODUCT_NOT_FOUND(404, "BENEFIT_PRODUCT_NOT_FOUND", "权益商品不存在或已下架"),
    BENEFIT_QUOTA_EXHAUSTED(409, "BENEFIT_QUOTA_EXHAUSTED", "本期权益名额已用尽"),
    BENEFIT_CARD_OUT_OF_STOCK(409, "BENEFIT_CARD_OUT_OF_STOCK", "卡密库存不足"),
    BENEFIT_CARD_INVALID(409, "BENEFIT_CARD_INVALID", "卡片失效，请联系客服"),
    PLAN_NOT_FOUND(404, "PLAN_NOT_FOUND", "套餐不存在或已下架"),
    ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND", "订单不存在"),
    ORDER_PENDING_EXISTS(409, "ORDER_PENDING_EXISTS", "已有未完成订单，请勿重复下单"),
    ORDER_STATE_INVALID(409, "ORDER_STATE_INVALID", "订单状态不允许该操作"),
    ORDER_REFUNDING(409, "ORDER_REFUNDING", "订单退款处理中，暂时无法领取"),
    PAYMENT_CHANNEL_UNSUPPORTED(409, "PAYMENT_CHANNEL_UNSUPPORTED", "支付渠道暂未开通"),
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
