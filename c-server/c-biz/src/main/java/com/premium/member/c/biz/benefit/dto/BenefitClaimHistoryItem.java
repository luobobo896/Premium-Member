package com.premium.member.c.biz.benefit.dto;

import java.time.OffsetDateTime;

/**
 * 历史领取单，卡密只展示脱敏值。
 */
public record BenefitClaimHistoryItem(
        Long claimId,
        String productSku,
        String productName,
        String status,
        String cardMasked,
        OffsetDateTime claimedAt,
        String remark,
        String redemption) {

    public static BenefitClaimHistoryItem from(BenefitClaimView view) {
        return new BenefitClaimHistoryItem(
                view.getId(),
                view.getProductSku(),
                view.getProductName(),
                view.getStatus(),
                view.getCardMask(),
                view.getClaimedAt(),
                view.getRemark(),
                view.getRedemption());
    }
}
