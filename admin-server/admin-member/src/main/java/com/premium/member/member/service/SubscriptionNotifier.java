package com.premium.member.member.service;

import com.premium.member.member.entity.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订阅停用通知。当前只落日志；接短信/微信模板消息时替换这一处。
 */
@Component
public class SubscriptionNotifier {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionNotifier.class);

    public void suspended(Subscription subscription, String reason) {
        log.warn("subscription suspended, notify member={} reason={}",
                subscription.getMemberId(), reason);
    }
}
