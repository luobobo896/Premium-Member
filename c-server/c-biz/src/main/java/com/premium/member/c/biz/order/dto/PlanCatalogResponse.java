package com.premium.member.c.biz.order.dto;

import com.premium.member.c.biz.order.entity.PlanVersion;

/**
 * 游客可见的套餐目录，不暴露版本号与套餐编码。
 */
public record PlanCatalogResponse(
        long id,
        String name,
        long priceCents,
        int durationDays,
        int aiImageQuota,
        int aiVideoQuota,
        int monthlyPoints,
        int benefitQuota) {

    public static PlanCatalogResponse from(PlanVersion plan) {
        return new PlanCatalogResponse(plan.getId(), plan.getName(), plan.getPriceCents(),
                plan.getDurationDays(), plan.getAiImageQuota(), plan.getAiVideoQuota(),
                plan.getMonthlyPoints(), plan.getBenefitQuota());
    }
}
