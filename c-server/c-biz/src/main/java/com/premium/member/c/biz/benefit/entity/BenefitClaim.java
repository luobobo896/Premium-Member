package com.premium.member.c.biz.benefit.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权益领取单。
 */
@Data
@TableName("benefit_claim")
public class BenefitClaim {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long periodId;

    private String productSku;

    private String status;

    private Long cardId;

    private OffsetDateTime claimedAt;

    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
