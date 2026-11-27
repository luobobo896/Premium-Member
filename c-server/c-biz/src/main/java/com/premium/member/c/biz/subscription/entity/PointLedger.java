package com.premium.member.c.biz.subscription.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("point_ledger")
public class PointLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Integer delta;

    private Integer balanceAfter;

    private String type;

    private String refType;

    private String refId;

    private String period;

    private String idempotencyKey;

    private String operator;

    private OffsetDateTime createdAt;
}
