package com.premium.member.c.biz.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premium.member.c.biz.ai.provider.AiProvider;
import com.premium.member.c.biz.ai.provider.DashScopeAiProvider;
import com.premium.member.c.biz.ai.provider.MockAiProvider;
import com.premium.member.c.biz.ai.provider.UnsupportedAiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 未匹配到供应商实现时显式失败，避免静默使用 Mock。
 */
@Configuration
@EnableConfigurationProperties(DashScopeProperties.class)
public class AiProviderConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "dashscope")
    public AiProvider dashScopeAiProvider(DashScopeProperties properties,
            ObjectMapper objectMapper) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            return new MockAiProvider();
        }
        return new DashScopeAiProvider(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AiProvider.class)
    public AiProvider unsupportedAiProvider() {
        return new UnsupportedAiProvider();
    }
}
