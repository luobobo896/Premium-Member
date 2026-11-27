package com.premium.member.framework.cache;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * refresh token 白名单与 access token 黑名单。
 */
@Service
public class TokenStore {

    private final StringRedisTemplate redisTemplate;
    private final CacheKeys cacheKeys;

    public TokenStore(StringRedisTemplate redisTemplate, CacheKeys cacheKeys) {
        this.redisTemplate = redisTemplate;
        this.cacheKeys = cacheKeys;
    }

    /**
     * 记录 refresh token 为可兑换状态。
     *
     * @param jti    refresh token 唯一标识
     * @param userId 用户 ID
     * @param ttl    与 token 相同的有效期
     */
    public void saveRefreshToken(String jti, long userId, Duration ttl) {
        redisTemplate.opsForValue().set(cacheKeys.refreshToken(jti), String.valueOf(userId), ttl);
    }

    /**
     * 判断 refresh token 是否仍可兑换。
     *
     * @param jti    refresh token 唯一标识
     * @param userId 用户 ID
     * @return true 表示白名单命中
     */
    public boolean isRefreshTokenActive(String jti, long userId) {
        String value = redisTemplate.opsForValue().get(cacheKeys.refreshToken(jti));
        return String.valueOf(userId).equals(value);
    }

    public void revokeRefreshToken(String jti) {
        redisTemplate.delete(cacheKeys.refreshToken(jti));
    }

    /**
     * 注销 access token；TTL 与 token 剩余寿命一致。
     *
     * @param jti  access token 唯一标识
     * @param ttl  剩余有效期
     */
    public void revokeAccessToken(String jti, Duration ttl) {
        if (jti != null && ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(cacheKeys.revokedAccessToken(jti), "1", ttl);
        }
    }

    public boolean isAccessTokenRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKeys.revokedAccessToken(jti)));
    }

    /**
     * 撤销用户在指定时间之前签发的全部 access token，用于前端 logout 不携带 Authorization 的场景。
     *
     * @param userId 用户 ID
     * @param ttl    保留撤销记录至少一个 access token 生命周期
     */
    public void revokeUserAccessTokens(long userId, Duration ttl) {
        redisTemplate.opsForValue().set(
                cacheKeys.userAccessRevokedAt(userId),
                String.valueOf(Instant.now().getEpochSecond()),
                ttl);
    }

    /**
     * 判断 access token 是否签发于用户最近一次登出之前。
     *
     * @param userId   用户 ID
     * @param issuedAt token 签发时间
     * @return true 表示已失效
     */
    public boolean isUserAccessRevoked(long userId, Instant issuedAt) {
        String value = redisTemplate.opsForValue().get(cacheKeys.userAccessRevokedAt(userId));
        if (value == null || issuedAt == null) {
            return false;
        }
        return issuedAt.getEpochSecond() <= Long.parseLong(value);
    }
}
