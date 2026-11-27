package com.premium.member.member.provider;

import com.premium.member.member.entity.Subscription;
import org.springframework.stereotype.Component;

/**
 * 微信代扣占位实现。真实接入需要商户号、委托代扣资质与签约模板。
 */
@Component
public class WechatPayWithholdProvider implements WithholdProvider {

    @Override
    public String name() {
        return "wechat";
    }

    @Override
    public WithholdResult withhold(Subscription subscription, String period, long amountCents,
            String mockResult) {
        return WithholdResult.fail("微信代扣未接入，需商户资质与签约模板");
    }
}
