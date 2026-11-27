package com.premium.member.c.biz.ai.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import com.premium.member.c.biz.ai.entity.Asset;
import com.premium.member.c.biz.ai.entity.AssetExpose;
import com.premium.member.c.biz.ai.mapper.AiMapper;
import com.premium.member.c.biz.ai.provider.AiProviderException;
import com.premium.member.c.biz.ai.storage.StorageService;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 为 AI 生成任务临时暴露输入素材，令牌不可枚举且限时、限次。
 */
@Service
public class AssetExposeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AiMapper aiMapper;
    private final StorageService storageService;
    private final String publicBaseUrl;
    private final long ttlSeconds;
    private final int downloads;

    public AssetExposeService(AiMapper aiMapper, StorageService storageService,
            @Value("${app.ai.public-asset-base-url:}") String publicBaseUrl,
            @Value("${app.ai.expose-ttl-seconds:600}") long ttlSeconds,
            @Value("${app.ai.expose-downloads:3}") int downloads) {
        this.aiMapper = aiMapper;
        this.storageService = storageService;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        this.ttlSeconds = ttlSeconds;
        this.downloads = downloads;
    }

    public ExposedInput expose(Asset asset) {
        String signed = storageService.signedUrl(asset.getStorageKey(), Duration.ofSeconds(ttlSeconds));
        if (StringUtils.hasText(signed)) {
            return new ExposedInput(signed, null);
        }
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_INPUT_UNREACHABLE,
                    ErrorCode.AI_PROVIDER_INPUT_UNREACHABLE.code()
                            + " PUBLIC_ASSET_BASE_URL 未配置");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AssetExpose expose = new AssetExpose();
        expose.setToken(token);
        expose.setAssetId(asset.getId());
        expose.setExpireAt(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttlSeconds));
        expose.setRemainingDownloads(downloads);
        aiMapper.insertExpose(expose);
        return new ExposedInput(publicBaseUrl + "/c/assets/" + token, token);
    }

    public void revoke(String token) {
        if (StringUtils.hasText(token)) {
            aiMapper.deleteExpose(token);
        }
    }

    @Transactional
    public PublicAsset open(String token) {
        AssetExpose expose = aiMapper.lockExpose(token);
        if (expose == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Asset asset = aiMapper.selectAsset(expose.getAssetId());
        if (asset == null || !"ACTIVE".equals(asset.getStatus())
                || (asset.getExpireAt() != null
                        && asset.getExpireAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC)))) {
            aiMapper.deleteExpose(token);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (aiMapper.consumeExpose(token) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        aiMapper.incrementExposeCount(asset.getId());
        Resource resource = storageService.load(asset.getStorageKey());
        return new PublicAsset(resource, asset.getMime());
    }

    public record PublicAsset(Resource resource, String mime) {
    }

    public record ExposedInput(String url, String token) {
    }
}
