package com.premium.member.c.biz.ai.dto;

import java.time.OffsetDateTime;

import com.premium.member.c.biz.subscription.dto.SubscriptionView;

/**
 * C 端 AI 积分额度与能力单价。
 */
public record QuotaResponse(
        int balancePoints,
        int monthlyPoints,
        String period,
        OffsetDateTime resetAt,
        Prices prices,
        SubscriptionView subscription) {

    public record Prices(int photo, int dressup, int video) {
    }
}
