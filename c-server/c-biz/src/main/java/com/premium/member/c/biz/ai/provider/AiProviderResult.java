package com.premium.member.c.biz.ai.provider;

/**
 * 供应商产出文件。
 */
public record AiProviderResult(
        byte[] content,
        String mime,
        String extension,
        String supplierTaskNo) {
}
