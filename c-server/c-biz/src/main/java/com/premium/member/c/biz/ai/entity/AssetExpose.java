package com.premium.member.c.biz.ai.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 临时暴露给 AI 供应商的输入素材令牌。
 */
@Data
@TableName("asset_expose")
public class AssetExpose {

    @TableId
    private String token;

    private Long assetId;

    private OffsetDateTime expireAt;

    private Integer remainingDownloads;

    private OffsetDateTime createdAt;
}
