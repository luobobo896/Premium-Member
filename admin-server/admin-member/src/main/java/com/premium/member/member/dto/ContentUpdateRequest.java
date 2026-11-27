package com.premium.member.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 编辑草稿内容。
 */
public record ContentUpdateRequest(@NotBlank String contentJson) {
}
