package com.premium.member.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存内容草稿；同一 key+locale 已有草稿时更新草稿，否则新建版本。
 */
public record ContentSaveRequest(
        @NotBlank String key,
        String locale,
        @NotBlank String contentJson) {
}
