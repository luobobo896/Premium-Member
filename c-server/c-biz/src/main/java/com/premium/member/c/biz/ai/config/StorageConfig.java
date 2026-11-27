package com.premium.member.c.biz.ai.config;

import com.premium.member.c.biz.ai.storage.LocalStorageService;
import com.premium.member.c.biz.ai.storage.OssStorageService;
import com.premium.member.c.biz.ai.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 配置完整时使用 OSS，否则回退本地目录。
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class StorageConfig {

    @Bean
    public StorageService storageService(OssProperties ossProperties,
            @Value("${app.ai.storage-dir:./data/ai}") String storageDir) {
        if (ossProperties.configured()) {
            return new OssStorageService(ossProperties);
        }
        return new LocalStorageService(storageDir);
    }
}
