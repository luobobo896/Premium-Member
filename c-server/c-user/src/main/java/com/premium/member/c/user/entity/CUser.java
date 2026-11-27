package com.premium.member.c.user.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * C 端账号。
 */
@Data
@TableName("c_user")
public class CUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String passwordHash;

    private String nickname;

    private String avatar;

    /** 1 启用，0 停用。 */
    private Integer status;

    private OffsetDateTime createdAt;
}
