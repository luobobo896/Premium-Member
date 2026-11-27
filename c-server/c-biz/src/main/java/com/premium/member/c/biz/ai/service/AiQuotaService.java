package com.premium.member.c.biz.ai.service;

import com.premium.member.c.biz.ai.dto.QuotaResponse;
import com.premium.member.c.biz.subscription.dto.SubscriptionView;
import com.premium.member.c.biz.subscription.service.PointService;
import org.springframework.stereotype.Service;

/**
 * AI 积分：提交时消费，失败时退还。次数额度已废弃，只保留历史数据。
 */
@Service
public class AiQuotaService {

    private final PointService pointService;
    private final PointPriceService priceService;

    public AiQuotaService(PointService pointService, PointPriceService priceService) {
        this.pointService = pointService;
        this.priceService = priceService;
    }

    public QuotaResponse forSubscription(SubscriptionView subscription) {
        int balance = subscription == null ? 0 : subscription.balancePoints();
        int monthly = subscription == null ? 0 : subscription.monthlyPoints();
        return new QuotaResponse(balance, monthly, PointService.currentPeriod(),
                subscription == null ? null : subscription.nextChargeAt(),
                new QuotaResponse.Prices(priceService.photoPrice(), priceService.dressupPrice(),
                        priceService.videoPrice()),
                subscription);
    }

    public int price(String type) {
        return priceService.price(type);
    }

    public void consume(long memberId, String type, String taskNo) {
        pointService.consume(memberId, price(type), taskNo, PointService.currentPeriod());
    }

    public void refund(long memberId, String type, String taskNo) {
        pointService.refund(memberId, price(type), taskNo, PointService.currentPeriod());
    }
}
