package com.premium.member.framework.security;

import java.time.Instant;

/**
 * refresh token 解析结果。
 *
 * @param userId    用户 ID
 * @param username  用户名
 * @param jti       唯一标识
 * @param expiresAt 过期时间
 */
public record RefreshClaims(long userId, String username, String jti, Instant expiresAt) {
}
