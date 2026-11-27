package com.premium.member.member.controller;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.BillingRunRequest;
import com.premium.member.member.dto.BillingRunResponse;
import com.premium.member.member.dto.ChargeQuery;
import com.premium.member.member.dto.ChargeRow;
import com.premium.member.member.dto.GrantPointsRequest;
import com.premium.member.member.dto.SubscriptionQuery;
import com.premium.member.member.dto.SubscriptionRow;
import com.premium.member.member.service.SubscriptionAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端订阅、代扣记录与积分补发。
 */
@RestController
@RequestMapping("/system/subscriptions")
public class SystemSubscriptionController {

    private final SubscriptionAdminService subscriptionService;

    public SystemSubscriptionController(SubscriptionAdminService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('subscription:list')")
    public R<PageResult<SubscriptionRow>> subscriptions(@Valid SubscriptionQuery query) {
        return R.ok(subscriptionService.subscriptions(query));
    }

    @GetMapping("/charges")
    @PreAuthorize("hasAuthority('subscription:charge:list')")
    public R<PageResult<ChargeRow>> charges(@Valid ChargeQuery query) {
        return R.ok(subscriptionService.charges(query));
    }

    @PostMapping("/{id}/grant-points")
    @AuditLog(module = "订阅", action = "手动补发积分")
    @PreAuthorize("hasAuthority('subscription:grant')")
    public R<Void> grant(@PathVariable long id, @Valid @RequestBody GrantPointsRequest request) {
        subscriptionService.grant(id, request);
        return R.ok();
    }

    @PostMapping("/run-billing")
    @AuditLog(module = "订阅", action = "执行 1 号任务")
    @PreAuthorize("hasAuthority('subscription:run')")
    public R<BillingRunResponse> runBilling(@RequestBody(required = false) BillingRunRequest request) {
        return R.ok(subscriptionService.runBilling(request == null
                ? new BillingRunRequest(null, null, null, null, null) : request));
    }

    @PostMapping("/run-dunning")
    @AuditLog(module = "订阅", action = "执行宽限重试")
    @PreAuthorize("hasAuthority('subscription:run')")
    public R<BillingRunResponse> runDunning(@RequestBody(required = false) BillingRunRequest request) {
        return R.ok(subscriptionService.runDunning(request == null
                ? new BillingRunRequest(null, null, null, null, false) : request));
    }
}
