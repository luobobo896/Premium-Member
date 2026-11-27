package com.premium.member.member.provider;

import java.util.Locale;
import java.util.UUID;

import com.premium.member.member.entity.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 未接入真实微信代扣前的 Mock。mockResult=FAIL 可模拟失败。
 */
@Component
public class MockWithholdProvider implements WithholdProvider {

    private final String defaultResult;

    public MockWithholdProvider(@Value("${app.subscription.mock-result:SUCCESS}") String result) {
        this.defaultResult = result;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public WithholdResult withhold(Subscription subscription, String period, long amountCents,
            String mockResult) {
        String result = mockResult == null || mockResult.isBlank()
                ? defaultResult : mockResult;
        if ("FAIL".equals(result.toUpperCase(Locale.ROOT))) {
            return WithholdResult.fail("模拟代扣失败");
        }
        return WithholdResult.ok("MOCK-WH-" + period.replace("-", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
