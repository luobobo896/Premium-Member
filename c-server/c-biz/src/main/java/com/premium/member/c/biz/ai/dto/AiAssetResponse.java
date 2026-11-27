package com.premium.member.c.biz.ai.dto;

import com.premium.member.c.biz.ai.entity.Asset;

/**
 * 上传后的素材。
 */
public record AiAssetResponse(long id, String purpose, String mime, long size, String url) {

    public static AiAssetResponse from(Asset asset, String url) {
        return new AiAssetResponse(asset.getId(), asset.getPurpose(), asset.getMime(),
                asset.getSize(), url);
    }
}
