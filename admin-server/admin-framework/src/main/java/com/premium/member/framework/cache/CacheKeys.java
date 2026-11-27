package com.premium.member.framework.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis key 统一构造，前缀包含环境，避免多环境共用实例时冲突。
 */
@Component
public class CacheKeys {

    private final String prefix;

    public CacheKeys(@Value("${app.cache-prefix:pm:dev}") String prefix) {
        this.prefix = prefix;
    }

    public String refreshToken(String jti) {
        return prefix + ":auth:refresh:" + jti;
    }

    public String revokedAccessToken(String jti) {
        return prefix + ":auth:revoked:" + jti;
    }

    public String userPermissions(long userId) {
        return prefix + ":auth:perm:" + userId;
    }

    public String userAccessRevokedAt(long userId) {
        return prefix + ":auth:user-revoked:" + userId;
    }

    public String captcha(String captchaId) {
        return prefix + ":auth:captcha:" + captchaId;
    }
}
