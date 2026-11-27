package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 卡密。codeCipher 仅在服务端解密，不进入响应。
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

    /** UNUSED / CLAIMED / DISABLED。 */
    private String status;

    private OffsetDateTime expireAt;

    private OffsetDateTime claimedAt;

    private Long claimedBy;

    private Long claimId;

    private OffsetDateTime createdAt;
}
