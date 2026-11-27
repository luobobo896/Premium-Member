package com.premium.member.c.framework.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * refresh token 白名单，与 admin 使用不同的 key 前缀。
 */
@Service
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "c:refresh:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String jti, long userId, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, String.valueOf(userId), ttl);
    }

    public boolean isActive(String jti, long userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + jti);
        return value != null && value.equals(String.valueOf(userId));
    }

    public void revoke(String jti) {
        redisTemplate.delete(KEY_PREFIX + jti);
    }
}
