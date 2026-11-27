package com.premium.member.c.biz.ticket.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

/**
 * C 端工单；列表不返回会话。
 */
@Data
public class CTicketView {

    private Long id;
    private String ticketNo;
    private String orderNo;
    private String category;
    private String title;
    private String content;
    private String status;
    private String priority;
    private Long replyCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime resolvedAt;
    private List<CTicketReplyView> replies;
}
