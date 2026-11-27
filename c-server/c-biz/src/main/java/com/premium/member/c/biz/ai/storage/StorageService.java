package com.premium.member.c.biz.ai.storage;

import java.time.Duration;

import org.springframework.core.io.Resource;

/**
 * 文件存储抽象。dev 使用本地目录，生产替换为对象存储实现。
 */
public interface StorageService {

    StoredFile store(byte[] content, String extension);

    default StoredFile store(byte[] content, String extension, String category) {
        return store(content, extension);
    }

    Resource load(String storageKey);

    void delete(String storageKey);

    /**
     * 远端存储返回短期签名 URL，本地存储返回 null 由业务走本地回退路径。
     */
    default String signedUrl(String storageKey, Duration ttl) {
        return null;
    }

    default boolean remote() {
        return false;
    }

    /**
     * 存储结果。
     *
     * @param storageKey 相对存储键
     */
    record StoredFile(String storageKey) {
    }
}
