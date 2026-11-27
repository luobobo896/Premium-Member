package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class ChargeRow {

    private Long id;
    private String chargeNo;
    private Long subscriptionId;
    private Long memberId;
    private String memberNo;
    private String memberName;
    private String phone;
    private String period;
    private Long amountCents;
    private String status;
    private String provider;
    private String channelTxnNo;
    private String failReason;
    private Integer attemptNo;
    private OffsetDateTime createdAt;
    private OffsetDateTime finishedAt;
}
