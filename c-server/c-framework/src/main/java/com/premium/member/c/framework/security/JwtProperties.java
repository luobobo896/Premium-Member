package com.premium.member.c.framework.security;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * C 端 JWT 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** access token 签名密钥，至少 32 字节。 */
    private String accessSecret = "";

    /** refresh token 签名密钥，至少 32 字节。 */
    private String refreshSecret = "";

    /** issuer 声明。 */
    private String issuer = "premium-member-c";

    /** access token 有效期，默认 2 小时。 */
    private Duration accessTtl = Duration.ofHours(2);

    /** refresh token 有效期，默认 30 天。 */
    private Duration refreshTtl = Duration.ofDays(30);
}
