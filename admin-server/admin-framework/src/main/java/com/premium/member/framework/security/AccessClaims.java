package com.premium.member.framework.security;

import java.time.Instant;
import java.util.List;

/**
 * access token 解析结果。
 *
 * @param user        认证主体
 * @param permissions 权限码
 * @param jti         唯一标识
 * @param expiresAt   过期时间
 */
public record AccessClaims(AuthenticatedUser user, List<String> permissions, String jti, Instant expiresAt) {
}
