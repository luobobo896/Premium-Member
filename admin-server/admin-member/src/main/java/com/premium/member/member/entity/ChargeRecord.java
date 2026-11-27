package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("charge_record")
public class ChargeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String chargeNo;

    private Long subscriptionId;

    private Long memberId;

    private String period;

    private Long amountCents;

    private String status;

    private String provider;

    private String channelTxnNo;

    private String failReason;

    private Integer attemptNo;

    private OffsetDateTime createdAt;

    private OffsetDateTime finishedAt;
}
