package com.premium.member.c.biz.ai.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.premium.member.c.biz.ai.dto.AiAssetResponse;
import com.premium.member.c.biz.ai.entity.Asset;
import com.premium.member.c.biz.ai.mapper.AiMapper;
import com.premium.member.c.biz.ai.storage.StorageService;
import com.premium.member.c.biz.member.entity.PmMember;
import com.premium.member.c.biz.member.mapper.PmMemberMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 素材上传与读取。
 */
@Service
public class AiAssetService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/quicktime", "video/webm");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "video/mp4", "mp4",
            "video/quicktime", "mov",
            "video/webm", "webm");

    private final AiMapper aiMapper;
    private final PmMemberMapper memberMapper;
    private final StorageService storageService;

    public AiAssetService(AiMapper aiMapper, PmMemberMapper memberMapper,
            StorageService storageService) {
        this.aiMapper = aiMapper;
        this.memberMapper = memberMapper;
        this.storageService = storageService;
    }

    public AiAssetResponse upload(String phone, MultipartFile file) {
        PmMember member = requireActiveMember(phone);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_ASSET_UNSUPPORTED, "请选择要上传的图片或视频");
        }
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME.contains(mime)) {
            throw new BusinessException(ErrorCode.AI_ASSET_UNSUPPORTED);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件读取失败");
        }
        StorageService.StoredFile stored = storageService.store(content, EXTENSIONS.get(mime));
        Asset asset = new Asset();
        asset.setOwnerMemberId(member.getId());
        asset.setPurpose("INPUT");
        asset.setMime(mime);
        asset.setSize((long) content.length);
        asset.setStorageKey(stored.storageKey());
        asset.setStatus("ACTIVE");
        asset.setExpireAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
        aiMapper.insertAsset(asset);
        return AiAssetResponse.from(asset);
    }

    public Resource load(String storageKey) {
        Asset asset = aiMapper.selectActiveAssetByKey(storageKey);
        if (asset == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return storageService.load(storageKey);
    }

    private PmMember requireActiveMember(String phone) {
        PmMember member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);
        }
        return member;
    }
}
