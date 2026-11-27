package com.premium.member.c.biz.ai.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 创作任务。
 */
@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long memberId;

    private Long periodId;

    private Long templateId;

    private String type;

    private String status;

    private Long inputAssetId;

    private Long outputAssetId;

    private String supplierTaskNo;

    private String failReason;

    private Long costCents;

    private String idempotencyKey;

    private String mockResult;

    private OffsetDateTime createdAt;

    private OffsetDateTime finishedAt;
}
