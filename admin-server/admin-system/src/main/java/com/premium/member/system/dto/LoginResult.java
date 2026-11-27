package com.premium.member.system.dto;

/**
 * 登录服务内部结果：响应体和需要写入 Cookie 的 refresh token。
 *
 * @param body         登录响应
 * @param refreshToken refresh token
 */
public record LoginResult(LoginResponse body, String refreshToken) {
}
