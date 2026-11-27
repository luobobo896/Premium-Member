package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import com.premium.member.member.entity.AiTemplate;

/**
 * B 端 AI 模板响应。
 */
public record AiTemplateResponse(
        long id,
        String code,
        String name,
        String type,
        String category,
        String coverUrl,
        String params,
        int status,
        int versionNo,
        int sort,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static AiTemplateResponse from(AiTemplate template) {
        return new AiTemplateResponse(template.getId(), template.getCode(), template.getName(),
                template.getType(), template.getCategory(), template.getCoverUrl(),
                template.getParams() == null ? "{}" : template.getParams(), template.getStatus(),
                template.getVersionNo(), template.getSort(), template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
