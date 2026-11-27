package com.premium.member.c.biz.ai.controller;

import com.premium.member.c.biz.ai.dto.QuotaResponse;
import com.premium.member.c.biz.ai.service.AiTaskService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端 AI 额度。
 */
@RestController
@RequestMapping("/c/quota")
public class CQuotaController {

    private final AiTaskService taskService;

    public CQuotaController(AiTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public R<QuotaResponse> quota() {
        return R.ok(taskService.quota(SecurityUtils.currentUser().phone()));
    }
}
