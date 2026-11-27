package com.premium.member.c.biz.subscription.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("point_account")
public class PointAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Integer balancePoints;

    private OffsetDateTime updatedAt;
}
