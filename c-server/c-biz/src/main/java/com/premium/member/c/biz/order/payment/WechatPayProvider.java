package com.premium.member.c.biz.order.payment;

import com.premium.member.c.biz.order.entity.Order;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 微信 H5 支付预留。商户号、证书和密钥只允许来自环境变量。
 */
@Component
public class WechatPayProvider implements PaymentProvider {

    @Override
    public String channel() {
        return "WECHAT_H5";
    }

    @Override
    public String createPayment(Order order) {
        throw new BusinessException(ErrorCode.PAYMENT_CHANNEL_UNSUPPORTED);
    }
}
