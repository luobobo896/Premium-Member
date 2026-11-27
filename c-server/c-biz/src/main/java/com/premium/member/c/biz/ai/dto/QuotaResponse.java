package com.premium.member.c.biz.ai.dto;

import com.premium.member.c.biz.order.dto.PeriodView;

/**
 * C 端 AI 额度。
 */
public record QuotaResponse(PeriodView period, QuotaPart image, QuotaPart video) {

    public record QuotaPart(int limit, int used, int remaining) {
    }
}
