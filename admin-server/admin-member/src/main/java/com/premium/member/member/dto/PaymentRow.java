package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 支付流水列表行。
 */
@Data
public class PaymentRow {

    private Long id;
    private String txnNo;
    private Long orderId;
    private String orderNo;
    private String memberNo;
    private String memberName;
    private String channel;
    private Long amountCents;
    private String status;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}
