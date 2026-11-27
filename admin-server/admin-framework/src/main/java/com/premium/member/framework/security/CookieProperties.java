package com.premium.member.framework.security;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * refresh token Cookie 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    /** Cookie 名，vben mock 使用 jwt。 */
    private String name = "jwt";

    /** Cookie 路径，默认与 API 上下文路径一致。 */
    private String path = "/api";

    /** HTTPS 环境必须为 true，且 SameSite=None。 */
    private boolean secure = false;

    /** Cookie 有效期。 */
    private Duration maxAge = Duration.ofDays(30);
}
