package com.premium.member.framework.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户权限码缓存，减少每次登录/刷新时的多表查询。
 */
@Service
public class PermissionCache {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final CacheKeys cacheKeys;

    public PermissionCache(StringRedisTemplate redisTemplate, CacheKeys cacheKeys) {
        this.redisTemplate = redisTemplate;
        this.cacheKeys = cacheKeys;
    }

    public Optional<List<String>> get(long userId) {
        String value = redisTemplate.opsForValue().get(cacheKeys.userPermissions(userId));
        if (value == null) {
            return Optional.empty();
        }
        List<String> permissions = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        return Optional.of(permissions);
    }

    public void put(long userId, List<String> permissions) {
        redisTemplate.opsForValue().set(cacheKeys.userPermissions(userId), String.join(",", permissions), TTL);
    }

    public void evict(long userId) {
        redisTemplate.delete(cacheKeys.userPermissions(userId));
    }
}
