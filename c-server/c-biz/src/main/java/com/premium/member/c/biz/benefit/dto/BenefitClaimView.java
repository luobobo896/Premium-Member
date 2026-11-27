package com.premium.member.c.biz.benefit.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 领取单展示行，带商品名与卡密脱敏值。
 */
@Data
public class BenefitClaimView {

    private Long id;
    private String productSku;
    private String productName;
    private String status;
    private String cardMask;
    private String redemption;
    private OffsetDateTime claimedAt;
    private String remark;
    private OffsetDateTime createdAt;
}
