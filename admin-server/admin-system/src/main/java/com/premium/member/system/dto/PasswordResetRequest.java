package com.premium.member.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求。
 *
 * @param password 新密码，6-64 位
 */
public record PasswordResetRequest(@NotBlank @Size(min = 6, max = 64) String password) {
}
