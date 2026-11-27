package com.premium.member.c.biz.order.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 在售套餐版本。历史订单引用版本行，不覆盖。
 */
@Data
@TableName("plan_version")
public class PlanVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Long priceCents;

    private Integer durationDays;

    private Integer aiImageQuota;

    private Integer aiVideoQuota;

    private Integer benefitQuota;

    private Integer status;

    private Integer versionNo;

    private OffsetDateTime createdAt;
}
