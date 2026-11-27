package com.premium.member.c.biz.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premium.member.c.biz.ai.provider.AiProvider;
import com.premium.member.c.biz.ai.provider.DashScopeAiProvider;
import com.premium.member.c.biz.ai.provider.MockAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AiProviderConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AiProviderConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void dashScopeWithoutKeyFallsBackToMock() {
        runner.withPropertyValues("app.ai.provider=dashscope")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(AiProvider.class).size());
                    assertInstanceOf(MockAiProvider.class, context.getBean(AiProvider.class));
                });
    }

    @Test
    void dashScopeWithKeyUsesRealProvider() {
        runner.withPropertyValues("app.ai.provider=dashscope",
                        "app.ai.dashscope.api-key=test-key")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(AiProvider.class).size());
                    assertInstanceOf(DashScopeAiProvider.class, context.getBean(AiProvider.class));
                });
    }
}
