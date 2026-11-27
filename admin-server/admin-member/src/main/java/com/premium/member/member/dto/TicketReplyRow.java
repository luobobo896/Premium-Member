package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 工单回复。
 */
@Data
public class TicketReplyRow {

    private Long id;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String content;
    private OffsetDateTime createdAt;
}
