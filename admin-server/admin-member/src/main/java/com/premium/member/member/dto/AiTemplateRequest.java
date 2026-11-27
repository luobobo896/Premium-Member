package com.premium.member.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增或编辑 AI 模板。
 */
public record AiTemplateRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Pattern(regexp = "PHOTO|DRESSUP|VIDEO") String type,
        @Size(max = 64) String category,
        @Size(max = 512) String coverUrl,
        String params,
        @Min(0) @Max(1) Integer status,
        Integer sort) {
}
