package com.premium.member.framework.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

/**
 * JWT 签发与解析。access token 携带角色和权限，refresh token 只携带身份。
 */
@Service
public class JwtTokenService {

    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 签发 access token。
     *
     * @param user 登录用户
     * @return token 与过期时间
     */
    public TokenValue createAccessToken(AuthUser user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(properties.getAccessTtl());
        String token = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(user.userId()))
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("type", "access")
                .claim("uid", user.userId())
                .claim("username", user.username())
                .claim("realName", user.realName())
                .claim("roles", user.roles())
                .claim("perms", user.permissions())
                .signWith(accessKey())
                .compact();
        return new TokenValue(token, jti, expiresAt);
    }

    /**
     * 签发 refresh token。
     *
     * @param user 登录用户
     * @return token 与过期时间
     */
    public TokenValue createRefreshToken(AuthUser user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(properties.getRefreshTtl());
        String token = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(user.userId()))
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("type", "refresh")
                .claim("uid", user.userId())
                .claim("username", user.username())
                .signWith(refreshKey())
                .compact();
        return new TokenValue(token, jti, expiresAt);
    }

    /**
     * 校验并解析 access token，同时构造认证主体。
     *
     * @param token JWT
     * @return 认证主体
     */
    public AccessClaims parseAccessToken(String token) {
        Claims claims = parse(token, accessKey(), "access");
        long userId = numberClaim(claims, "uid");
        String username = claims.get("username", String.class);
        String realName = claims.get("realName", String.class);
        List<String> roles = stringList(claims, "roles");
        List<String> permissions = stringList(claims, "perms");
        AuthenticatedUser user = new AuthenticatedUser(userId, username, realName, roles);
        return new AccessClaims(user, permissions, claims.getId(), claims.getExpiration().toInstant());
    }

    /**
     * 校验并解析 refresh token。
     *
     * @param token JWT
     * @return refresh token 声明
     */
    public RefreshClaims parseRefreshToken(String token) {
        Claims claims = parse(token, refreshKey(), "refresh");
        return new RefreshClaims(
                numberClaim(claims, "uid"),
                claims.get("username", String.class),
                claims.getId(),
                claims.getExpiration().toInstant());
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

    private long numberClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }

    private List<String> stringList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private SecretKey accessKey() {
        return key(properties.getAccessSecret(), "JWT access secret");
    }

    private SecretKey refreshKey() {
        return key(properties.getRefreshSecret(), "JWT refresh secret");
    }

    private SecretKey key(String secret, String name) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, name + " 未配置或长度不足 32 字节");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
