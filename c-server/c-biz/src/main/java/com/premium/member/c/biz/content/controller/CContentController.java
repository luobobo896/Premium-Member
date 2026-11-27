package com.premium.member.c.biz.content.controller;

import java.util.List;

import com.premium.member.c.biz.content.dto.CContentResponse;
import com.premium.member.c.biz.content.service.CContentService;
import com.premium.member.c.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端内容配置。
 */
@RestController
@RequestMapping("/c/contents")
public class CContentController {

    private final CContentService contentService;

    public CContentController(CContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public R<List<CContentResponse>> published(
            @RequestParam(required = false) List<String> keys,
            @RequestParam(required = false) String locale) {
        return R.ok(contentService.published(keys, locale));
    }
}
