package com.premium.member.framework.security;

import java.time.Instant;

/**
 * 已签发的 token 及其元数据。
 *
 * @param value     JWT 字符串
 * @param jti       唯一标识
 * @param expiresAt 过期时间
 */
public record TokenValue(String value, String jti, Instant expiresAt) {
}
