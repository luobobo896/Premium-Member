package com.premium.member.c.biz.ai.config;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云百炼 DashScope 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.ai.dashscope")
public class DashScopeProperties {

    private String apiKey = "";

    private String baseUrl = "https://dashscope.aliyuncs.com";

    private String imageModel = "wanx2.1-t2i-turbo";

    private String editModel = "wanx2.1-imageedit";

    private String videoModel = "wan2.5-i2v-preview";

    private Duration pollInterval = Duration.ofSeconds(3);

    private Duration imageTimeout = Duration.ofMinutes(2);

    private Duration videoTimeout = Duration.ofMinutes(10);
}
