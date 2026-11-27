package com.premium.member.c.biz.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 开发环境模拟支付回调。
 */
public record MockNotifyRequest(@NotBlank String orderNo) {
}
