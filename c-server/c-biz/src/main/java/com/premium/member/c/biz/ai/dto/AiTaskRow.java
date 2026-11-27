package com.premium.member.c.biz.ai.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * C 端任务联表行。
 */
@Data
public class AiTaskRow {

    private Long id;
    private String taskNo;
    private Long periodId;
    private Long templateId;
    private String templateName;
    private String type;
    private String status;
    private Long inputAssetId;
    private String inputStorageKey;
    private Long outputAssetId;
    private String outputStorageKey;
    private String outputMime;
    private String failReason;
    private Long costCents;
    private OffsetDateTime createdAt;
    private OffsetDateTime finishedAt;
}
