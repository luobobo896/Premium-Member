package com.premium.member.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.member.dto.BillingRunRequest;
import com.premium.member.member.dto.BillingRunResponse;
import com.premium.member.member.dto.ChargeQuery;
import com.premium.member.member.dto.ChargeRow;
import com.premium.member.member.dto.GrantPointsRequest;
import com.premium.member.member.dto.SubscriptionQuery;
import com.premium.member.member.dto.SubscriptionRow;
import com.premium.member.member.mapper.SubscriptionMapper;
import org.springframework.stereotype.Service;

/**
 * B 端订阅、代扣记录与手动补发。
 */
@Service
public class SubscriptionAdminService {

    private final SubscriptionMapper mapper;
    private final SubscriptionBillingService billingService;
    private final SubscriptionRunner runner;

    public SubscriptionAdminService(SubscriptionMapper mapper,
            SubscriptionBillingService billingService, SubscriptionRunner runner) {
        this.mapper = mapper;
        this.billingService = billingService;
        this.runner = runner;
    }

    public PageResult<SubscriptionRow> subscriptions(SubscriptionQuery query) {
        Page<SubscriptionRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<SubscriptionRow> result = mapper.selectSubscriptionPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public PageResult<ChargeRow> charges(ChargeQuery query) {
        Page<ChargeRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<ChargeRow> result = mapper.selectChargePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public void grant(long subscriptionId, GrantPointsRequest request) {
        billingService.grantManual(subscriptionId, request.points(), request.reason().trim(),
                SecurityUtils.currentUser().username());
    }

    public BillingRunResponse runBilling(BillingRunRequest request) {
        return runner.runBilling(request);
    }

    public BillingRunResponse runDunning(BillingRunRequest request) {
        return runner.runDunning(request.subscriptionId(), request.mockResult(),
                Boolean.TRUE.equals(request.forceSuspend()));
    }
}
