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
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "用户不存在"),
    ROLE_NOT_FOUND(404, "ROLE_NOT_FOUND", "角色不存在"),
    MENU_NOT_FOUND(404, "MENU_NOT_FOUND", "菜单不存在"),
    CONFLICT(409, "CONFLICT", "资源冲突"),
    MEMBER_NO_EXISTS(409, "MEMBER_NO_EXISTS", "会员编号已存在"),
    USERNAME_EXISTS(409, "USERNAME_EXISTS", "登录名已存在"),
    ROLE_CODE_EXISTS(409, "ROLE_CODE_EXISTS", "角色码已存在"),
    ROLE_IN_USE(409, "ROLE_IN_USE", "角色已分配给用户，不能删除"),
    DEPT_HAS_CHILDREN(409, "DEPT_HAS_CHILDREN", "存在子部门，不能删除"),
    DEPT_IN_USE(409, "DEPT_IN_USE", "部门下存在用户，不能删除"),
    MENU_HAS_CHILDREN(409, "MENU_HAS_CHILDREN", "存在子菜单，不能删除"),
    MENU_IN_USE(409, "MENU_IN_USE", "菜单已被角色引用，不能删除"),
    BENEFIT_PRODUCT_NOT_FOUND(404, "BENEFIT_PRODUCT_NOT_FOUND", "权益商品不存在"),
    BENEFIT_SKU_EXISTS(409, "BENEFIT_SKU_EXISTS", "SKU 编码已存在"),
    BENEFIT_SKU_IMMUTABLE(409, "BENEFIT_SKU_IMMUTABLE", "SKU 编码创建后不可修改"),
    BENEFIT_CARD_NOT_FOUND(404, "BENEFIT_CARD_NOT_FOUND", "卡密不存在"),
    BENEFIT_CARD_OUT_OF_STOCK(409, "BENEFIT_CARD_OUT_OF_STOCK", "卡密库存不足"),
    BENEFIT_CLAIM_NOT_FOUND(404, "BENEFIT_CLAIM_NOT_FOUND", "领取单不存在"),
    BENEFIT_CLAIM_STATE_INVALID(409, "BENEFIT_CLAIM_STATE_INVALID", "领取单状态不允许该操作"),
    PLAN_NOT_FOUND(404, "PLAN_NOT_FOUND", "套餐不存在"),
    ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND", "订单不存在"),
    ORDER_STATE_INVALID(409, "ORDER_STATE_INVALID", "订单状态不允许该操作"),
    REFUND_ALREADY_EXISTS(409, "REFUND_ALREADY_EXISTS", "该订单已有处理中的退款"),
    REFUND_AMOUNT_EXCEEDS(409, "REFUND_AMOUNT_EXCEEDS", "退款金额不能超过实付金额"),
    REFUND_CLAIM_PENDING(409, "REFUND_CLAIM_PENDING", "订单周期仍有处理中或状态未知的领取单"),
    PAYMENT_CHANNEL_UNSUPPORTED(409, "PAYMENT_CHANNEL_UNSUPPORTED", "该支付渠道暂不支持自动退款"),
    USER_PROTECTED(403, "USER_PROTECTED", "内置用户或当前登录用户受保护"),
    ROLE_PROTECTED(403, "ROLE_PROTECTED", "super_admin 角色受保护"),
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
