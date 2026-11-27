package com.premium.member.c.biz.benefit.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 卡密。
 */
@Data
@TableName("benefit_card")
public class BenefitCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String productSku;

    private String codeHash;

    private String codeCipher;

    private String codeMask;

    private String status;

    private OffsetDateTime expireAt;
}
