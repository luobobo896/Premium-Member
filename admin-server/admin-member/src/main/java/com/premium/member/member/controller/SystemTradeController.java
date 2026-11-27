package com.premium.member.member.controller;

import java.util.List;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.OrderDetailResponse;
import com.premium.member.member.dto.OrderQuery;
import com.premium.member.member.dto.OrderRow;
import com.premium.member.member.dto.PaymentQuery;
import com.premium.member.member.dto.PaymentRow;
import com.premium.member.member.dto.PlanRequest;
import com.premium.member.member.dto.PlanResponse;
import com.premium.member.member.dto.RefundRequest;
import com.premium.member.member.service.TradeAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端套餐、订单、退款与支付流水。
 */
@RestController
@RequestMapping("/system")
public class SystemTradeController {

    private final TradeAdminService tradeService;

    public SystemTradeController(TradeAdminService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('plan:list')")
    public R<List<PlanResponse>> plans() {
        return R.ok(tradeService.plans());
    }

    @PostMapping("/plans")
    @AuditLog(module = "交易", action = "新增套餐")
    @PreAuthorize("hasAuthority('plan:create')")
    public R<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return R.ok(tradeService.createPlan(request));
    }

    @PutMapping("/plans/{id}")
    @AuditLog(module = "交易", action = "生成套餐新版本")
    @PreAuthorize("hasAuthority('plan:update')")
    public R<PlanResponse> updatePlan(@PathVariable long id,
            @Valid @RequestBody PlanRequest request) {
        return R.ok(tradeService.updatePlan(id, request));
    }

    @DeleteMapping("/plans/{id}")
    @AuditLog(module = "交易", action = "下架套餐")
    @PreAuthorize("hasAuthority('plan:delete')")
    public R<Void> deletePlan(@PathVariable long id) {
        tradeService.deletePlan(id);
        return R.ok();
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('order:list')")
    public R<PageResult<OrderRow>> orders(@Valid OrderQuery query) {
        return R.ok(tradeService.orders(query));
    }

    @GetMapping("/orders/{orderNo}")
    @PreAuthorize("hasAuthority('order:list')")
    public R<OrderDetailResponse> order(@PathVariable String orderNo) {
        return R.ok(tradeService.order(orderNo));
    }

    @PostMapping("/orders/{orderNo}/refund")
    @AuditLog(module = "交易", action = "退款审核")
    @PreAuthorize("hasAuthority('order:refund')")
    public R<Void> refund(@PathVariable String orderNo,
            @Valid @RequestBody RefundRequest request) {
        tradeService.refund(orderNo, request);
        return R.ok();
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('payment:list')")
    public R<PageResult<PaymentRow>> payments(@Valid PaymentQuery query) {
        return R.ok(tradeService.payments(query));
    }
}
