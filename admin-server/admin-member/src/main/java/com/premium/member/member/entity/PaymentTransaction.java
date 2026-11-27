package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 支付交易流水。
 */
@Data
@TableName("payment_transactions")
public class PaymentTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txnNo;

    private Long orderId;

    private String channel;

    private Long amountCents;

    private String status;

    private String rawPayload;

    private OffsetDateTime paidAt;

    private OffsetDateTime createdAt;
}
