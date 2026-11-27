package com.premium.member.c.biz.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 下单只接收套餐版本，金额由服务端读取。
 */
public record CreateOrderRequest(
        @NotNull Long planVersionId,
        @Size(max = 32) String source) {
}
