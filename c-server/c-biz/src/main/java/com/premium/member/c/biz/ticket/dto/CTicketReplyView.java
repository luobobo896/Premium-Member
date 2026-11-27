package com.premium.member.c.biz.ticket.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * C 端可见的工单回复；内部备注不下发。
 */
@Data
public class CTicketReplyView {

    private String senderType;
    private String senderName;
    private String content;
    private OffsetDateTime createdAt;
}
