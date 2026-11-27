package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员周期，退款成功时终止。
 */
@Data
@TableName("pm_member_period")
public class PmMemberPeriod {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private String periodNo;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private String status;

    private Long orderId;
}
