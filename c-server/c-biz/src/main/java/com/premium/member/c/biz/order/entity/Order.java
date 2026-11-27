package com.premium.member.c.biz.order.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员套餐订单。
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long memberId;

    private Long userId;

    private Long planVersionId;

    private Long amountCents;

    private String status;

    private String channel;

    private String source;

    private String purpose;

    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime paidAt;

    private OffsetDateTime updatedAt;
}
