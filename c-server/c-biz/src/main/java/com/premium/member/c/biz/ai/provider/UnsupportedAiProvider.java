package com.premium.member.c.biz.ai.provider;

/**
 * 真实供应商接入前的占位实现，显式失败而不是静默回退。
 */
public class UnsupportedAiProvider implements AiProvider {

    @Override
    public String name() {
        return "unsupported";
    }

    @Override
    public AiProviderResult generate(AiProviderRequest request) {
        throw new UnsupportedOperationException("AI 供应商尚未接入");
    }
}
