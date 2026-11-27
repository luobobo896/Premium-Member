package com.premium.member.c.framework.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

/**
 * C 端 JWT 签发与解析。
 */
@Service
public class JwtTokenService {

    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成 access token。
     *
     * @param userId 用户 ID
     * @param phone  手机号
     * @return JWT 字符串
     */
    public String createAccessToken(long userId, String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTtl())))
                .claim("type", "access")
                .claim("uid", userId)
                .claim("phone", phone)
                .signWith(accessKey())
                .compact();
    }

    /**
     * 生成 refresh token。
     *
     * @param userId 用户 ID
     * @param phone  手机号
     * @return JWT 字符串
     */
    public String createRefreshToken(long userId, String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getRefreshTtl())))
                .claim("type", "refresh")
                .claim("uid", userId)
                .claim("phone", phone)
                .signWith(refreshKey())
                .compact();
    }

    /**
     * 校验并解析 access token。
     *
     * @param token JWT
     * @return claims
     */
    public Claims parseAccessToken(String token) {
        return parse(token, accessKey(), "access");
    }

    /**
     * 校验并解析 refresh token。
     *
     * @param token JWT
     * @return claims
     */
    public Claims parseRefreshToken(String token) {
        return parse(token, refreshKey(), "refresh");
    }

    private Claims parse(String token, SecretKey key, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get("type", String.class))) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    private SecretKey accessKey() {
        return key(properties.getAccessSecret());
    }

    private SecretKey refreshKey() {
        return key(properties.getRefreshSecret());
    }

    private SecretKey key(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 密钥未配置或长度不足 32 字节");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
