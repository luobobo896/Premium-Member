package com.premium.member.member.provider;

import com.premium.member.member.entity.Subscription;

/**
 * 自动续费代扣渠道。
 */
public interface WithholdProvider {

    String name();

    WithholdResult withhold(Subscription subscription, String period, long amountCents,
            String mockResult);
}
