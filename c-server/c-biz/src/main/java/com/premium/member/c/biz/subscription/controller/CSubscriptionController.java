package com.premium.member.c.biz.subscription.controller;

import com.premium.member.c.biz.subscription.dto.SubscriptionView;
import com.premium.member.c.biz.subscription.service.SubscriptionService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端订阅信息。
 */
@RestController
@RequestMapping("/c/subscription")
public class CSubscriptionController {

    private final SubscriptionService subscriptionService;

    public CSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public R<SubscriptionView> current() {
        return R.ok(subscriptionService.view(SecurityUtils.currentUser().phone()));
    }
}
