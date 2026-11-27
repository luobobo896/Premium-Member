package com.premium.member.member.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.ContentItemResponse;
import com.premium.member.member.dto.ContentSaveRequest;
import com.premium.member.member.dto.ContentUpdateRequest;
import com.premium.member.member.service.ContentAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端内容配置。
 */
@RestController
@RequestMapping("/system/contents")
public class SystemContentController {

    private final ContentAdminService contentService;

    public SystemContentController(ContentAdminService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('content:list')")
    public R<List<ContentItemResponse>> list(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String locale) {
        return R.ok(contentService.list(key, locale));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('content:list')")
    public R<ContentItemResponse> preview(@PathVariable long id) {
        return R.ok(contentService.preview(id));
    }

    @PostMapping
    @AuditLog(module = "内容配置", action = "保存草稿")
    @PreAuthorize("hasAuthority('content:update')")
    public R<ContentItemResponse> saveDraft(@Valid @RequestBody ContentSaveRequest request) {
        return R.ok(contentService.saveDraft(request));
    }

    @PutMapping("/{id}")
    @AuditLog(module = "内容配置", action = "编辑草稿")
    @PreAuthorize("hasAuthority('content:update')")
    public R<ContentItemResponse> updateDraft(@PathVariable long id,
            @Valid @RequestBody ContentUpdateRequest request) {
        return R.ok(contentService.updateDraft(id, request.contentJson()));
    }

    @PostMapping("/{id}/publish")
    @AuditLog(module = "内容配置", action = "发布")
    @PreAuthorize("hasAuthority('content:publish')")
    public R<ContentItemResponse> publish(@PathVariable long id) {
        return R.ok(contentService.publish(id));
    }

    @PostMapping("/{id}/rollback")
    @AuditLog(module = "内容配置", action = "回滚")
    @PreAuthorize("hasAuthority('content:rollback')")
    public R<ContentItemResponse> rollback(@PathVariable long id) {
        return R.ok(contentService.rollback(id));
    }
}
