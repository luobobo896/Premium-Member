package com.premium.member.c.biz.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 提交 AI 创作任务。
 */
public record AiTaskCreateRequest(
        @NotNull Long templateId,
        @NotNull Long inputAssetId,
        @NotBlank @Size(max = 128) String idempotencyKey,
        @Size(max = 16) String mockResult) {
}
