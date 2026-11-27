package com.premium.member.c.biz.ai.dto;

import com.premium.member.c.biz.ai.entity.AiTemplate;

/**
 * C 端 AI 模板。
 */
public record AiTemplateResponse(
        long id,
        String code,
        String name,
        String type,
        String category,
        String coverUrl,
        String params,
        int sort,
        int pointPrice) {

    public static AiTemplateResponse from(AiTemplate template, int pointPrice) {
        return new AiTemplateResponse(template.getId(), template.getCode(), template.getName(),
                template.getType(), template.getCategory(), template.getCoverUrl(),
                template.getParams() == null ? "{}" : template.getParams(), template.getSort(),
                pointPrice);
    }
}
