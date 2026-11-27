package com.premium.member.c.biz.benefit.dto;

import java.time.OffsetDateTime;

/**
 * 领取结果。cardCode 只在本次新领取成功时返回一次。
 *
 * @param alreadyClaimed true 表示重复点击命中同一领取单，不再返回明文
 */
public record BenefitClaimResult(
        Long claimId,
        String productSku,
        String productName,
        String status,
        boolean alreadyClaimed,
        String cardCode,
        String cardMasked,
        OffsetDateTime claimedAt,
        String redemption) {
}
