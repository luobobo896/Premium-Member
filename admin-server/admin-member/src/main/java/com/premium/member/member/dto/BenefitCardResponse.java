package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import com.premium.member.member.entity.BenefitCard;

/**
 * 卡密响应，只返回脱敏值。
 */
public record BenefitCardResponse(
        Long id,
        String batchNo,
        String productSku,
        String codeMasked,
        String status,
        OffsetDateTime expireAt,
        OffsetDateTime claimedAt,
        Long claimedBy,
        OffsetDateTime createdAt) {

    public static BenefitCardResponse from(BenefitCard card) {
        return new BenefitCardResponse(
                card.getId(),
                card.getBatchNo(),
                card.getProductSku(),
                card.getCodeMask(),
                card.getStatus(),
                card.getExpireAt(),
                card.getClaimedAt(),
                card.getClaimedBy(),
                card.getCreatedAt());
    }
}
