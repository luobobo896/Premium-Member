package com.premium.member.c.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册参数。
 *
 * @param phone    手机号
 * @param password 明文密码
 * @param nickname 昵称，可为空
 */
public record RegisterRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
        String password,
        @Size(max = 64, message = "昵称不能超过 64 个字符")
        String nickname) {
}
