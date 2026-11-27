package com.premium.member.member.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import com.premium.member.member.dto.BillingRunRequest;
import com.premium.member.member.dto.BillingRunResponse;
import com.premium.member.member.entity.Subscription;
import com.premium.member.member.mapper.SubscriptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 批量驱动订阅任务。每个订阅单独事务，单个失败不影响其他订阅。
 */
@Service
public class SubscriptionRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRunner.class);
    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int BATCH = 200;

    private final SubscriptionMapper mapper;
    private final SubscriptionBillingService billingService;

    public SubscriptionRunner(SubscriptionMapper mapper,
            SubscriptionBillingService billingService) {
        this.mapper = mapper;
        this.billingService = billingService;
    }

    public BillingRunResponse runBilling(BillingRunRequest request) {
        String mockResult = request.mockResult();
        Integer graceDays = request.graceDays();
        String period = request.period();
        int processed = 0;
        int granted = 0;
        int failed = 0;
        int suspended = 0;
        if (request.subscriptionId() != null) {
            Subscription subscription = mapper.selectById(request.subscriptionId());
            if (subscription != null) {
                period = period == null ? manualPeriod(subscription) : period;
                processed++;
                try {
                    SubscriptionBillingService.Outcome outcome = billingService.renew(
                            subscription.getId(), period, mockResult, graceDays, true);
                    granted += outcome == SubscriptionBillingService.Outcome.GRANTED ? 1 : 0;
                    failed += outcome == SubscriptionBillingService.Outcome.FAILED ? 1 : 0;
                    suspended += outcome == SubscriptionBillingService.Outcome.SUSPENDED ? 1 : 0;
                } catch (RuntimeException ex) {
                    log.warn("subscription billing failed, id={}", subscription.getId(), ex);
                }
            }
        } else {
            List<Subscription> due = mapper.selectDue(OffsetDateTime.now(BILLING_ZONE), BATCH);
            for (Subscription subscription : due) {
                if (period == null) {
                    period = periodOf(subscription);
                }
                processed++;
                try {
                    SubscriptionBillingService.Outcome outcome = billingService.renew(
                            subscription.getId(), null, mockResult, graceDays, false);
                    granted += outcome == SubscriptionBillingService.Outcome.GRANTED ? 1 : 0;
                    failed += outcome == SubscriptionBillingService.Outcome.FAILED ? 1 : 0;
                    suspended += outcome == SubscriptionBillingService.Outcome.SUSPENDED ? 1 : 0;
                } catch (RuntimeException ex) {
                    log.warn("subscription billing failed, id={}", subscription.getId(), ex);
                }
            }
        }
        return new BillingRunResponse(period == null ? "" : period, processed, granted, failed,
                suspended);
    }

    public BillingRunResponse runDunning(Long subscriptionId, String mockResult,
            boolean forceSuspend) {
        int processed = 0;
        int failed = 0;
        int suspended = 0;
        if (subscriptionId != null) {
            processed++;
            SubscriptionBillingService.Outcome outcome = billingService.retryOrSuspend(
                    subscriptionId, mockResult, forceSuspend);
            failed += outcome == SubscriptionBillingService.Outcome.FAILED ? 1 : 0;
            suspended += outcome == SubscriptionBillingService.Outcome.SUSPENDED ? 1 : 0;
        } else {
            List<Subscription> pastDue = mapper.selectPastDue(BATCH);
            for (Subscription subscription : pastDue) {
                processed++;
                try {
                    SubscriptionBillingService.Outcome outcome = billingService.retryOrSuspend(
                            subscription.getId(), mockResult, false);
                    failed += outcome == SubscriptionBillingService.Outcome.FAILED ? 1 : 0;
                    suspended += outcome == SubscriptionBillingService.Outcome.SUSPENDED ? 1 : 0;
                } catch (RuntimeException ex) {
                    log.warn("subscription dunning failed, id={}", subscription.getId(), ex);
                }
            }
        }
        return new BillingRunResponse("", processed, 0, failed, suspended);
    }

    private String periodOf(Subscription subscription) {
        if (subscription.getNextChargeAt() == null) {
            return YearMonth.now(BILLING_ZONE).toString();
        }
        return YearMonth.from(subscription.getNextChargeAt().atZoneSameInstant(BILLING_ZONE))
                .toString();
    }

    /**
     * 手动重跑按“今天对应的账期”取，保证同一天重复点击仍是同一个 period。
     */
    private String manualPeriod(Subscription subscription) {
        OffsetDateTime now = OffsetDateTime.now(BILLING_ZONE);
        if (subscription.getNextChargeAt() != null
                && !subscription.getNextChargeAt().isAfter(now)) {
            return periodOf(subscription);
        }
        LocalDate today = now.toLocalDate();
        return YearMonth.from(today).plusMonths(today.getDayOfMonth() == 1 ? 0 : 1).toString();
    }
}
