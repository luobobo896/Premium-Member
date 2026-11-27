package com.premium.member.member.dto;

import java.util.List;

import com.premium.member.member.entity.PaymentTransaction;
import com.premium.member.member.entity.Refund;

/**
 * 订单详情：套餐快照、周期、支付流水与退款记录。
 */
public record OrderDetailResponse(
        OrderRow order,
        List<PaymentTransaction> payments,
        List<Refund> refunds) {
}
