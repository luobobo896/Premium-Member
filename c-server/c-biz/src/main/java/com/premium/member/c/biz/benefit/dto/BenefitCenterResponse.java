package com.premium.member.c.biz.benefit.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * C 端权益中心。
 */
public record BenefitCenterResponse(
        Period period,
        int quotaTotal,
        int quotaUsed,
        int quotaRemaining,
        List<Product> products) {

    public record Period(Long id, String periodNo, OffsetDateTime startAt, OffsetDateTime endAt) {
    }

    /**
     * @param claimed    当前周期内该 SKU 已有领取单（无论成功或失败）
     * @param inStock    仍有可用卡密
     */
    public record Product(
            String skuCode,
            String name,
            String description,
            String rules,
            boolean claimed,
            String claimStatus,
            String cardMasked,
            OffsetDateTime claimedAt,
            boolean inStock) {
    }
}
