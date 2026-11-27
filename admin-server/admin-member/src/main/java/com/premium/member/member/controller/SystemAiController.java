package com.premium.member.member.controller;

import java.time.LocalDate;
import java.util.List;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.AiTaskQuery;
import com.premium.member.member.dto.AiTaskRow;
import com.premium.member.member.dto.AiTemplateRequest;
import com.premium.member.member.dto.AiTemplateResponse;
import com.premium.member.member.dto.AiUsageRow;
import com.premium.member.member.service.AiAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端 AI 模板、任务与用量。
 */
@RestController
@RequestMapping("/system/ai")
public class SystemAiController {

    private final AiAdminService aiService;

    public SystemAiController(AiAdminService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('ai:template:list')")
    public R<List<AiTemplateResponse>> templates() {
        return R.ok(aiService.templates());
    }

    @PostMapping("/templates")
    @AuditLog(module = "AI 创作", action = "新增模板")
    @PreAuthorize("hasAuthority('ai:template:create')")
    public R<AiTemplateResponse> createTemplate(@Valid @RequestBody AiTemplateRequest request) {
        return R.ok(aiService.create(request));
    }

    @PutMapping("/templates/{id}")
    @AuditLog(module = "AI 创作", action = "编辑模板")
    @PreAuthorize("hasAuthority('ai:template:update')")
    public R<AiTemplateResponse> updateTemplate(@PathVariable long id,
            @Valid @RequestBody AiTemplateRequest request) {
        return R.ok(aiService.update(id, request));
    }

    @DeleteMapping("/templates/{id}")
    @AuditLog(module = "AI 创作", action = "删除模板")
    @PreAuthorize("hasAuthority('ai:template:delete')")
    public R<Void> deleteTemplate(@PathVariable long id) {
        aiService.delete(id);
        return R.ok();
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('ai:task:list')")
    public R<PageResult<AiTaskRow>> tasks(@Valid AiTaskQuery query) {
        return R.ok(aiService.tasks(query));
    }

    @GetMapping("/usage")
    @PreAuthorize("hasAuthority('ai:usage:list')")
    public R<List<AiUsageRow>> usage(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return R.ok(aiService.usage(from, to));
    }
}
