package com.premium.member.c.biz.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 阿里云 OSS 配置。未配置完整凭证时回退本地存储。
 */
@Data
@ConfigurationProperties(prefix = "app.ai.oss")
public class OssProperties {

    private String endpoint = "";

    private String bucket = "";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    private String prefix = "premium-member/ai";

    /**
     * 本地 stub 可用路径风格；生产保持 false。
     */
    private boolean pathStyle;

    public boolean configured() {
        return StringUtils.hasText(endpoint) && StringUtils.hasText(bucket)
                && StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret);
    }
}
