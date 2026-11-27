package com.premium.member.c.biz.member.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * admin 维护的会员表，C 端只读。
 */
@Data
@TableName("pm_member")
public class PmMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String memberNo;

    private String username;

    private String nickname;

    private String phone;

    private String level;

    private Integer points;

    private BigDecimal balance;

    private Integer status;

    private OffsetDateTime joinedAt;
}
