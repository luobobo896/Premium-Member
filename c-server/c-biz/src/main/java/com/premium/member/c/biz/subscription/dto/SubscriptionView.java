package com.premium.member.c.biz.subscription.dto;

import java.time.OffsetDateTime;

/**
 * C 端会员卡上的订阅信息。
 */
public record SubscriptionView(
        long id,
        long planId,
        String planName,
        String level,
        String memberNo,
        String status,
        String statusText,
        int monthlyPoints,
        long priceCents,
        int balancePoints,
        String period,
        OffsetDateTime nextChargeAt,
        OffsetDateTime lastChargeAt,
        OffsetDateTime graceUntil,
        String failureReason) {
}
