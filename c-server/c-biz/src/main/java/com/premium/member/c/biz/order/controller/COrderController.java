package com.premium.member.c.biz.order.controller;

import java.util.List;

import com.premium.member.c.biz.order.dto.CreateOrderRequest;
import com.premium.member.c.biz.order.dto.OrderResponse;
import com.premium.member.c.biz.order.dto.PlanResponse;
import com.premium.member.c.biz.order.service.OrderService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.CUserPrincipal;
import com.premium.member.c.framework.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端套餐与订单。
 */
@RestController
@RequestMapping("/c")
public class COrderController {

    private final OrderService orderService;

    public COrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/plans")
    public R<List<PlanResponse>> plans() {
        return R.ok(orderService.plans());
    }

    @PostMapping("/orders")
    public R<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        CUserPrincipal user = SecurityUtils.currentUser();
        return R.ok(orderService.create(user.phone(), user.userId(), request.planVersionId(),
                request.source()));
    }

    @GetMapping("/orders")
    public R<List<OrderResponse>> orders() {
        CUserPrincipal user = SecurityUtils.currentUser();
        return R.ok(orderService.list(user.phone(), user.userId()));
    }

    @GetMapping("/orders/{orderNo}")
    public R<OrderResponse> order(@PathVariable String orderNo) {
        CUserPrincipal user = SecurityUtils.currentUser();
        return R.ok(orderService.detail(user.phone(), user.userId(), orderNo));
    }
}
