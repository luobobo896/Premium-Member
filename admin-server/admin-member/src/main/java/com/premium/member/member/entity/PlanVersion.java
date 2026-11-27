package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 套餐版本。改价或改规则生成新版本，历史订单引用旧版本。
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

    private Integer monthlyPoints;

    private Integer benefitQuota;

    private Integer status;

    private Integer versionNo;

    private OffsetDateTime createdAt;
}
