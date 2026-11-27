package com.premium.member.member.service;

import com.premium.member.member.dto.BillingRunRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1 号 00:05 清零并代扣；每小时重试宽限期内失败的订阅。
 */
@Component
public class SubscriptionJob {

    private final SubscriptionRunner runner;

    @Value("${app.subscription.enabled:true}")
    private boolean enabled;

    public SubscriptionJob(SubscriptionRunner runner) {
        this.runner = runner;
    }

    @Scheduled(cron = "${app.subscription.billing-cron:0 5 0 1 * *}", zone = "Asia/Shanghai")
    public void monthly() {
        if (!enabled) {
            return;
        }
        runner.runBilling(new BillingRunRequest(null, null, null, null, null));
    }

    @Scheduled(cron = "${app.subscription.dunning-cron:0 30 * * * *}", zone = "Asia/Shanghai")
    public void dunning() {
        if (!enabled) {
            return;
        }
        runner.runDunning(null, null, false);
    }
}
