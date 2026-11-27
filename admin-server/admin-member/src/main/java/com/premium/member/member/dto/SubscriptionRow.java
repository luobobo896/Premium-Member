package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class SubscriptionRow {

    private Long id;
    private Long memberId;
    private String memberNo;
    private String memberName;
    private String phone;
    private String planName;
    private String status;
    private Integer monthlyPoints;
    private Long priceCents;
    private Integer balancePoints;
    private OffsetDateTime nextChargeAt;
    private OffsetDateTime lastChargeAt;
    private OffsetDateTime graceUntil;
    private String failureReason;
    private Integer version;
    private String lastChargeStatus;
    private String lastChargeReason;
    private OffsetDateTime lastChargeTime;
}
