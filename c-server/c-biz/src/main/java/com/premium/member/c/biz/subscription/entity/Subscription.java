package com.premium.member.c.biz.subscription.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员订阅。每月 1 号自动代扣并发放积分。
 */
@Data
@TableName("subscription")
public class Subscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long planId;

    private String status;

    private Integer monthlyPoints;

    private Long priceCents;

    private Integer billingDay;

    private OffsetDateTime nextChargeAt;

    private OffsetDateTime lastChargeAt;

    private OffsetDateTime graceUntil;

    private String failureReason;

    private String signContractNo;

    private Integer version;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
