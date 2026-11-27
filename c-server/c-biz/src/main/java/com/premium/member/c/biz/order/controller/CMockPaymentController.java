package com.premium.member.c.biz.order.controller;

import com.premium.member.c.biz.order.dto.MockNotifyRequest;
import com.premium.member.c.biz.order.dto.OrderResponse;
import com.premium.member.c.biz.order.service.OrderService;
import com.premium.member.c.common.api.R;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅 dev 暴露的模拟支付回调。
 */
@RestController
@Profile("dev")
@RequestMapping("/c/payments/mock")
public class CMockPaymentController {

    private final OrderService orderService;

    public CMockPaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/notify")
    public R<OrderResponse> notify(@Valid @RequestBody MockNotifyRequest request) {
        return R.ok(orderService.mockNotify(request.orderNo()));
    }
}
