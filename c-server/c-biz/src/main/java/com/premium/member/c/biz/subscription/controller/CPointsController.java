package com.premium.member.c.biz.subscription.controller;

import java.util.List;

import com.premium.member.c.biz.subscription.dto.PointLedgerView;
import com.premium.member.c.biz.subscription.service.SubscriptionService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端积分明细，可按 YYYY-MM 过滤。
 */
@RestController
@RequestMapping("/c/points")
public class CPointsController {

    private final SubscriptionService subscriptionService;

    public CPointsController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/ledger")
    public R<List<PointLedgerView>> ledger(@RequestParam(required = false) String month) {
        return R.ok(subscriptionService.ledger(SecurityUtils.currentUser().phone(), month));
    }
}
