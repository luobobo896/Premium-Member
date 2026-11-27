package com.premium.member.c.user.dto;

/**
 * 登录结果。
 *
 * @param accessToken access token
 * @param user        用户信息
 */
public record LoginResult(String accessToken, CUserInfoResponse user) {
}
