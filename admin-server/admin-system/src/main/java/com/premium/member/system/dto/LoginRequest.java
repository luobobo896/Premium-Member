package com.premium.member.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求，字段与 vben AuthApi.LoginParams 一致，额外支持可选服务端验证码。
 *
 * @param username    登录名
 * @param password    明文密码，仅用于本次校验
 * @param captchaId   验证码 ID，可选
 * @param captchaCode 验证码内容，可选
 */
public record LoginRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password,
        @Size(max = 64) String captchaId,
        @Size(max = 16) String captchaCode) {
}
