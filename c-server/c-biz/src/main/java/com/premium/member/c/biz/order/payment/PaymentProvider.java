package com.premium.member.c.biz.order.payment;

import com.premium.member.c.biz.order.entity.Order;

/**
 * 支付渠道抽象。密钥只从环境变量读取，不落库。
 */
public interface PaymentProvider {

    String channel();

    /**
     * 发起支付。返回客户端需要的支付模式，例如 MOCK 或 REDIRECT。
     */
    String createPayment(Order order);
}
