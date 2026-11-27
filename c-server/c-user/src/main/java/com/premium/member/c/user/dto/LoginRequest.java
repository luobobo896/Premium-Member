package com.premium.member.c.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登录参数。
 *
 * @param phone    手机号
 * @param password 明文密码
 */
public record LoginRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "密码不能为空")
        String password) {
}
