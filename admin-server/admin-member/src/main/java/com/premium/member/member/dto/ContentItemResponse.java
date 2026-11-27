package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import com.premium.member.member.entity.ContentItem;

/**
 * 内容配置版本。
 */
public record ContentItemResponse(
        long id,
        String key,
        String locale,
        String contentJson,
        int versionNo,
        String status,
        OffsetDateTime publishAt,
        String updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ContentItemResponse from(ContentItem item) {
        return new ContentItemResponse(item.getId(), item.getKey(), item.getLocale(),
                item.getContentJson(), item.getVersionNo(), item.getStatus(),
                item.getPublishAt(), item.getUpdatedBy(), item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
