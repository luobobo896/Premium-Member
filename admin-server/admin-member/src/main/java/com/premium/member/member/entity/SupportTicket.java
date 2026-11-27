package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 客服工单。
 */
@Data
@TableName("support_ticket")
public class SupportTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;

    private String userType;

    private Long memberId;

    private Long userId;

    private String orderNo;

    private String category;

    private String title;

    private String content;

    private String status;

    private String priority;

    private Long assigneeAdminId;

    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime resolvedAt;
}
