package com.premium.member.c.biz.order.dto;

import java.time.OffsetDateTime;

import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;

/**
 * C 端周期展示。
 */
public record PeriodView(
        long id,
        String periodNo,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status) {

    public static PeriodView from(PmMemberPeriod period) {
        if (period == null) {
            return null;
        }
        return new PeriodView(period.getId(), period.getPeriodNo(), period.getStartAt(),
                period.getEndAt(), period.getStatus());
    }
}
