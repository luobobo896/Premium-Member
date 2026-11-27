package com.premium.member.c.biz.order.payment;

import com.premium.member.c.biz.order.entity.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 仅开发环境使用的模拟支付。
 */
@Component
@Profile("dev")
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public String createPayment(Order order) {
        return "MOCK";
    }
}
