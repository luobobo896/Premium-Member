package com.premium.member.c.biz.order.dto;

import com.premium.member.c.biz.order.entity.PlanVersion;

/**
 * C 端在售套餐。
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
        int versionNo) {

    public static PlanResponse from(PlanVersion plan) {
        return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getPriceCents(),
                plan.getDurationDays(), plan.getAiImageQuota(), plan.getAiVideoQuota(),
                plan.getBenefitQuota(), plan.getVersionNo());
    }
}
