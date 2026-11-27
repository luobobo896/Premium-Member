package com.premium.member.c.biz.ai.dto;

import java.time.OffsetDateTime;

/**
 * C 端 AI 任务与作品。
 */
public record AiTaskResponse(
        long id,
        String taskNo,
        long periodId,
        long templateId,
        String templateName,
        String type,
        String status,
        Long inputAssetId,
        String inputUrl,
        Long outputAssetId,
        String outputUrl,
        String outputMime,
        String failReason,
        long costCents,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt) {
}
