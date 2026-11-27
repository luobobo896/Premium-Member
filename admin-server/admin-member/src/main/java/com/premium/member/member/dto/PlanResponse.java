package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import com.premium.member.member.entity.PlanVersion;

/**
 * B 端套餐版本。
 */
public record PlanResponse(
        long id,
        String code,
        String name,
        long priceCents,
        int durationDays,
        int aiImageQuota,
        int aiVideoQuota,
        int benefitQuota,
        int status,
        int versionNo,
        OffsetDateTime createdAt) {

    public static PlanResponse from(PlanVersion plan) {
        return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getPriceCents(),
                plan.getDurationDays(), plan.getAiImageQuota(), plan.getAiVideoQuota(),
                plan.getBenefitQuota(), plan.getStatus(), plan.getVersionNo(), plan.getCreatedAt());
    }
}
