package com.premium.member.c.biz.order.dto;

import java.time.OffsetDateTime;

import com.premium.member.c.biz.order.entity.Order;
import com.premium.member.c.biz.order.entity.PlanVersion;

/**
 * 订单详情，套餐字段来自 immutable 版本行。
 */
public record OrderResponse(
        long id,
        String orderNo,
        String status,
        long amountCents,
        String channel,
        String source,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        String payMode,
        PlanResponse plan,
        PeriodView period) {

    public static OrderResponse from(Order order, PlanVersion plan, String payMode, PeriodView period) {
        return new OrderResponse(order.getId(), order.getOrderNo(), order.getStatus(),
                order.getAmountCents(), order.getChannel(), order.getSource(), order.getRemark(),
                order.getCreatedAt(), order.getPaidAt(), payMode,
                plan == null ? null : PlanResponse.from(plan), period);
    }
}
