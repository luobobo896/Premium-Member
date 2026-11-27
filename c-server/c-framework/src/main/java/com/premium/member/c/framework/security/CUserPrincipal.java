package com.premium.member.c.framework.security;

/**
 * C 端登录用户。
 *
 * @param userId 用户 ID
 * @param phone  手机号
 */
public record CUserPrincipal(long userId, String phone) {
}
