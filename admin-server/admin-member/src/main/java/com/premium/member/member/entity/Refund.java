package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 整单退款申请与处理结果。
 */
@Data
@TableName("refunds")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long orderId;

    private Long amountCents;

    private String status;

    private String operator;

    private String reason;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime processedAt;
}
