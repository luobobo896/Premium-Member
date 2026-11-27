package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 工单列表行。
 */
@Data
public class TicketRow {

    private Long id;
    private String ticketNo;
    private String userType;
    private Long memberId;
    private String memberNo;
    private String memberName;
    private String phone;
    private Long userId;
    private String orderNo;
    private String category;
    private String title;
    private String content;
    private String status;
    private String priority;
    private Long assigneeAdminId;
    private String assigneeName;
    private String remark;
    private Long replyCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime resolvedAt;
}
