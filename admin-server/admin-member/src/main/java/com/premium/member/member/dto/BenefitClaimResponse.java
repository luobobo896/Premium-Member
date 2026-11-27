package com.premium.member.member.dto;

import java.time.OffsetDateTime;

/**
 * 领取单响应。
 */
public record BenefitClaimResponse(
        Long id,
        Long memberId,
        String memberNo,
        String memberName,
        Long periodId,
        String periodNo,
        String productSku,
        String productName,
        String status,
        String cardMasked,
        OffsetDateTime claimedAt,
        String remark,
        OffsetDateTime createdAt) {

    public static BenefitClaimResponse from(BenefitClaimRow row) {
        return new BenefitClaimResponse(
                row.getId(),
                row.getMemberId(),
                row.getMemberNo(),
                row.getMemberName(),
                row.getPeriodId(),
                row.getPeriodNo(),
                row.getProductSku(),
                row.getProductName(),
                row.getStatus(),
                row.getCardMask(),
                row.getClaimedAt(),
                row.getRemark(),
                row.getCreatedAt());
    }
}
