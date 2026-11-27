package com.premium.member.framework.security;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置，密钥必须由环境变量提供。
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** access token 签名密钥，至少 32 字节。 */
    private String accessSecret = "";

    /** refresh token 签名密钥，至少 32 字节。 */
    private String refreshSecret = "";

    /** issuer 声明。 */
    private String issuer = "premium-member-admin";

    /** access token 有效期。 */
    private Duration accessTtl = Duration.ofHours(2);

    /** refresh token 有效期。 */
    private Duration refreshTtl = Duration.ofDays(30);
}
