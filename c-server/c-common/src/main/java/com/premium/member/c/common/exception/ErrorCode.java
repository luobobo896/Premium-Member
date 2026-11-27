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
    AI_PERIOD_NOT_FOUND(409, "AI_PERIOD_NOT_FOUND", "当前没有生效中的 AI 创作周期"),
    AI_TEMPLATE_NOT_FOUND(404, "AI_TEMPLATE_NOT_FOUND", "AI 模板不存在或已下架"),
    AI_QUOTA_EXHAUSTED(409, "AI_QUOTA_EXHAUSTED", "本期 AI 额度已用尽"),
    AI_INPUT_REQUIRED(400, "AI_INPUT_REQUIRED", "请先上传输入素材"),
    AI_ASSET_NOT_FOUND(404, "AI_ASSET_NOT_FOUND", "素材不存在或已失效"),
    AI_ASSET_UNSUPPORTED(400, "AI_ASSET_UNSUPPORTED", "仅支持 JPG/PNG/WebP/GIF/MP4/MOV/WebM 文件"),
    AI_TASK_NOT_FOUND(404, "AI_TASK_NOT_FOUND", "AI 任务不存在"),
    AI_WORK_NOT_FOUND(404, "AI_WORK_NOT_FOUND", "作品不存在或不可删除"),
    AI_TASK_STATE_INVALID(409, "AI_TASK_STATE_INVALID", "AI 任务状态不允许该操作"),
    AI_IDEMPOTENCY_CONFLICT(409, "AI_IDEMPOTENCY_CONFLICT", "幂等键已被其他会员使用"),
    AI_PROVIDER_NOT_READY(503, "AI_PROVIDER_NOT_READY", "AI 供应商暂不可用"),
    AI_PROVIDER_NOT_ENABLED(409, "AI_PROVIDER_NOT_ENABLED", "AI 模型尚未开通"),
    AI_PROVIDER_RATE_LIMITED(429, "AI_PROVIDER_RATE_LIMITED", "AI 供应商限流或额度不足"),
    AI_PROVIDER_INPUT_UNREACHABLE(409, "AI_PROVIDER_INPUT_UNREACHABLE", "AI 供应商无法读取输入素材"),
    AI_PROVIDER_BAD_REQUEST(400, "AI_PROVIDER_BAD_REQUEST", "AI 供应商参数不合法"),
    AI_PROVIDER_TIMEOUT(504, "AI_PROVIDER_TIMEOUT", "AI 生成超时"),
    AI_PROVIDER_FAILED(502, "AI_PROVIDER_FAILED", "AI 供应商处理失败"),
    AI_CONTENT_REJECTED(422, "AI_CONTENT_REJECTED", "内容审核未通过"),
    TICKET_NOT_FOUND(404, "TICKET_NOT_FOUND", "工单不存在"),
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
