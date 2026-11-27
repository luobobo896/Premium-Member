package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权益领取单，(period_id, product_sku) 唯一。
 */
@Data
@TableName("benefit_claim")
public class BenefitClaim {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long periodId;

    private String productSku;

    /** AVAILABLE / PROCESSING / SUCCESS / FAILED / UNKNOWN。 */
    private String status;

    private Long cardId;

    private OffsetDateTime claimedAt;

    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
