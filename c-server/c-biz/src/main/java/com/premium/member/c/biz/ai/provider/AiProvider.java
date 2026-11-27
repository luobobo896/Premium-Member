package com.premium.member.c.biz.ai.provider;

/**
 * AI 供应商抽象。
 */
public interface AiProvider {

    String name();

    AiProviderResult generate(AiProviderRequest request);
}
