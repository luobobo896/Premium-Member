package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 领取单分页行，联表带会员、周期、商品与卡密脱敏值。
 */
@Data
public class BenefitClaimRow {

    private Long id;
    private Long memberId;
    private String memberNo;
    private String memberName;
    private Long periodId;
    private String periodNo;
    private String productSku;
    private String productName;
    private String status;
    private Long cardId;
    private String cardMask;
    private OffsetDateTime claimedAt;
    private String remark;
    private OffsetDateTime createdAt;
}
