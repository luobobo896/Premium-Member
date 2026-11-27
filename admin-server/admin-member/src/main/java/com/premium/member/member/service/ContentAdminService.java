package com.premium.member.member.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.member.dto.ContentItemResponse;
import com.premium.member.member.dto.ContentSaveRequest;
import com.premium.member.member.entity.ContentItem;
import com.premium.member.member.mapper.ContentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 内容配置版本：草稿可改，发布与回滚只新增版本。
 */
@Service
public class ContentAdminService {

    private static final Set<String> KEYS =
            Set.of("PLAN_COPY", "BANNER", "ANNOUNCEMENT", "DOWNLOAD", "CONTACT", "AGREEMENT");

    private final ContentMapper contentMapper;
    private final ObjectMapper objectMapper;

    public ContentAdminService(ContentMapper contentMapper, ObjectMapper objectMapper) {
        this.contentMapper = contentMapper;
        this.objectMapper = objectMapper;
    }

    public List<ContentItemResponse> list(String key, String locale) {
        String normalizedKey = normalizeKey(key);
        String normalizedLocale = normalizeLocale(locale);
        List<ContentItem> rows = normalizedKey == null
                ? contentMapper.selectLatest(null, normalizedLocale)
                : contentMapper.selectVersions(normalizedKey, normalizedLocale);
        return rows.stream().map(ContentItemResponse::from).toList();
    }

    public ContentItemResponse preview(long id) {
        return ContentItemResponse.from(requireItem(id));
    }

    @Transactional
    public ContentItemResponse saveDraft(ContentSaveRequest request) {
        String key = normalizeKey(request.key());
        if (key == null || !KEYS.contains(key)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "key 取值不合法");
        }
        String locale = normalizeLocale(request.locale());
        String content = validJson(request.contentJson());
        String operator = SecurityUtils.currentUser().username();
        ContentItem draft = contentMapper.selectLatestDraft(key, locale);
        if (draft != null) {
            contentMapper.updateDraft(draft.getId(), content, operator);
            return ContentItemResponse.from(contentMapper.selectById(draft.getId()));
        }
        ContentItem item = new ContentItem();
        item.setKey(key);
        item.setLocale(locale);
        item.setContentJson(content);
        item.setVersionNo(contentMapper.selectMaxVersion(key, locale) + 1);
        item.setUpdatedBy(operator);
        contentMapper.insert(item);
        return ContentItemResponse.from(contentMapper.selectById(item.getId()));
    }

    @Transactional
    public ContentItemResponse updateDraft(long id, String contentJson) {
        ContentItem item = requireItem(id);
        if (!"DRAFT".equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.CONTENT_STATE_INVALID);
        }
        contentMapper.updateDraft(id, validJson(contentJson),
                SecurityUtils.currentUser().username());
        return ContentItemResponse.from(contentMapper.selectById(id));
    }

    @Transactional
    public ContentItemResponse publish(long id) {
        ContentItem item = requireItem(id);
        if (!"DRAFT".equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.CONTENT_STATE_INVALID);
        }
        contentMapper.publish(id, SecurityUtils.currentUser().username());
        return ContentItemResponse.from(contentMapper.selectById(id));
    }

    @Transactional
    public ContentItemResponse rollback(long id) {
        ContentItem target = requireItem(id);
        if (!"PUBLISHED".equals(target.getStatus())) {
            throw new BusinessException(ErrorCode.CONTENT_STATE_INVALID);
        }
        int version = contentMapper.selectMaxVersion(target.getKey(), target.getLocale()) + 1;
        ContentItem copy = new ContentItem();
        copy.setId(target.getId());
        copy.setVersionNo(version);
        copy.setUpdatedBy(SecurityUtils.currentUser().username());
        contentMapper.insertRollback(copy);
        return ContentItemResponse.from(
                contentMapper.selectVersions(target.getKey(), target.getLocale()).stream()
                        .filter(row -> row.getVersionNo() == version)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND)));
    }

    private ContentItem requireItem(long id) {
        ContentItem item = contentMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        return item;
    }

    private String validJson(String json) {
        if (!StringUtils.hasText(json)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "contentJson 不能为空");
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "contentJson 必须是合法 JSON");
        }
        return json.trim();
    }

    private String normalizeKey(String key) {
        return StringUtils.hasText(key) ? key.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeLocale(String locale) {
        return StringUtils.hasText(locale) ? locale.trim() : "zh-CN";
    }
}
