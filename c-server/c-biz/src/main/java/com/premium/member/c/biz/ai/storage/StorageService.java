package com.premium.member.c.biz.ai.storage;

import org.springframework.core.io.Resource;

/**
 * 文件存储抽象。dev 使用本地目录，生产替换为对象存储实现。
 */
public interface StorageService {

    StoredFile store(byte[] content, String extension);

    Resource load(String storageKey);

    void delete(String storageKey);

    /**
     * 存储结果。
     *
     * @param storageKey 相对存储键
     */
    record StoredFile(String storageKey) {
    }
}
