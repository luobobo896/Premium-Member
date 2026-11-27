package com.premium.member.c.biz.content.service;

import java.util.List;
import java.util.Locale;

import com.premium.member.c.biz.content.dto.CContentResponse;
import com.premium.member.c.biz.content.mapper.CContentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * C 端内容读取。
 */
@Service
public class CContentService {

    private final CContentMapper contentMapper;

    public CContentService(CContentMapper contentMapper) {
        this.contentMapper = contentMapper;
    }

    public List<CContentResponse> published(List<String> keys, String locale) {
        List<String> normalized = keys == null ? List.of() : keys.stream()
                .filter(StringUtils::hasText)
                .map(key -> key.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        String normalizedLocale = StringUtils.hasText(locale) ? locale.trim() : "zh-CN";
        return contentMapper.selectPublished(normalized, normalizedLocale);
    }
}
