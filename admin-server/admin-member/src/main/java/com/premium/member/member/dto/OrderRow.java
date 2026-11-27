package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 订单列表/详情联表行。
 */
@Data
public class OrderRow {

    private Long id;
    private String orderNo;
    private Long memberId;
    private String memberNo;
    private String memberName;
    private String phone;
    private Long userId;
    private Long planVersionId;
    private String planCode;
    private String planName;
    private Integer planVersionNo;
    private Long planPriceCents;
    private Integer durationDays;
    private Integer aiImageQuota;
    private Integer aiVideoQuota;
    private Integer benefitQuota;
    private Long amountCents;
    private String status;
    private String channel;
    private String source;
    private String purpose;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime paidAt;
    private Long periodId;
    private String periodNo;
    private OffsetDateTime periodStartAt;
    private OffsetDateTime periodEndAt;
    private String periodStatus;
}
