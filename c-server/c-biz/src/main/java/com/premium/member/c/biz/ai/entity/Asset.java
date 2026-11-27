package com.premium.member.c.biz.ai.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户上传文件或 AI 产出文件。
 */
@Data
@TableName("asset")
public class Asset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerMemberId;

    private String purpose;

    private String mime;

    private Long size;

    private String storageKey;

    private String status;

    private OffsetDateTime expireAt;

    private OffsetDateTime createdAt;
}
