package com.premium.member.c.biz.ai.config;

import com.premium.member.c.biz.ai.provider.AiProvider;
import com.premium.member.c.biz.ai.provider.UnsupportedAiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 未匹配到供应商实现时显式失败，避免静默使用 Mock。
 */
@Configuration
public class AiProviderConfig {

    @Bean
    @ConditionalOnMissingBean(AiProvider.class)
    public AiProvider unsupportedAiProvider() {
        return new UnsupportedAiProvider();
    }
}
