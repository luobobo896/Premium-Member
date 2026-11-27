package com.premium.member.c.biz.ai.provider;

/**
 * 供应商调用参数。
 *
 * @param taskNo       任务号
 * @param type         PHOTO/DRESSUP/VIDEO
 * @param templateCode 模板编码
 * @param templateName 模板名称
 * @param inputMime    输入文件类型
 * @param inputBytes   输入文件内容
 * @param inputUrl     供应商可访问的输入文件 URL
 */
public record AiProviderRequest(
        String taskNo,
        String type,
        String templateCode,
        String templateName,
        String inputMime,
        byte[] inputBytes,
        String inputUrl) {
}
