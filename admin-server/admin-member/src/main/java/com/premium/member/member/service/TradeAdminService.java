package com.premium.member.member.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.member.dto.OrderDetailResponse;
import com.premium.member.member.dto.OrderQuery;
import com.premium.member.member.dto.OrderRow;
import com.premium.member.member.dto.PaymentQuery;
import com.premium.member.member.dto.PaymentRow;
import com.premium.member.member.dto.PlanRequest;
import com.premium.member.member.dto.PlanResponse;
import com.premium.member.member.dto.RefundRequest;
import com.premium.member.member.entity.Order;
import com.premium.member.member.entity.PlanVersion;
import com.premium.member.member.entity.PmMemberPeriod;
import com.premium.member.member.entity.Refund;
import com.premium.member.member.mapper.TradeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 套餐版本、订单与退款审核。
 */
@Service
public class TradeAdminService {

    private final TradeMapper tradeMapper;

    public TradeAdminService(TradeMapper tradeMapper) {
        this.tradeMapper = tradeMapper;
    }

    public List<PlanResponse> plans() {
        return tradeMapper.selectPlans().stream().map(PlanResponse::from).toList();
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        String code = normalizeCode(request.code());
        tradeMapper.lockPlanCode(code);
        PlanVersion plan = buildPlan(code, request, tradeMapper.maxPlanVersion(code) + 1);
        tradeMapper.disablePlanCode(code);
        tradeMapper.insertPlan(plan);
        return PlanResponse.from(plan);
    }

    /**
     * 任何修改都复制为新版本，旧版本保留给历史订单。
     */
    @Transactional
    public PlanResponse updatePlan(long id, PlanRequest request) {
        PlanVersion old = tradeMapper.selectPlan(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
        tradeMapper.lockPlanCode(old.getCode());
        PlanVersion plan = buildPlan(old.getCode(), request,
                tradeMapper.maxPlanVersion(old.getCode()) + 1);
        tradeMapper.disablePlanCode(old.getCode());
        tradeMapper.insertPlan(plan);
        return PlanResponse.from(plan);
    }

    @Transactional
    public void deletePlan(long id) {
        if (tradeMapper.selectPlan(id) == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
        tradeMapper.disablePlan(id);
    }

    public PageResult<OrderRow> orders(OrderQuery query) {
        Page<OrderRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<OrderRow> result = tradeMapper.selectOrderPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public OrderDetailResponse order(String orderNo) {
        OrderRow row = tradeMapper.selectOrderRow(orderNo);
        if (row == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return new OrderDetailResponse(row,
                tradeMapper.selectPaymentsByOrder(row.getId()),
                tradeMapper.selectRefundsByOrder(row.getId()));
    }

    @Transactional
    public void refund(String orderNo, RefundRequest request) {
        Order order = tradeMapper.lockOrder(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
        }
        if (tradeMapper.selectActiveRefund(order.getId()) != null) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_EXISTS);
        }
        if (request.amountCents() <= 0 || request.amountCents() > order.getAmountCents()) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDS);
        }
        String operator = SecurityUtils.currentUser().username();
        Refund refund = new Refund();
        refund.setRefundNo(newRefundNo());
        refund.setOrderId(order.getId());
        refund.setAmountCents(request.amountCents());
        refund.setOperator(operator);
        refund.setReason(request.reason().trim());
        refund.setStatus("REJECT".equals(request.result()) ? "REJECTED" : "APPLIED");
        tradeMapper.insertRefund(refund);
        if ("REJECT".equals(request.result())) {
            tradeMapper.updateRefundStatus(refund.getId(), "REJECTED");
            return;
        }
        if (!"MOCK".equals(order.getChannel())) {
            tradeMapper.updateRefundStatus(refund.getId(), "FAILED");
            throw new BusinessException(ErrorCode.PAYMENT_CHANNEL_UNSUPPORTED);
        }
        tradeMapper.updateOrderStatus(order.getId(), "REFUNDING");
        PmMemberPeriod period = tradeMapper.selectPeriodByOrder(order.getId());
        if (period != null && tradeMapper.countClaimInFlight(period.getId()) > 0) {
            throw new BusinessException(ErrorCode.REFUND_CLAIM_PENDING);
        }
        tradeMapper.updateRefundStatus(refund.getId(), "APPROVED");
        tradeMapper.updateRefundStatus(refund.getId(), "SUCCESS");
        tradeMapper.updateOrderStatus(order.getId(), "REFUNDED");
        tradeMapper.terminatePeriod(order.getId());
    }

    public PageResult<PaymentRow> payments(PaymentQuery query) {
        Page<PaymentRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<PaymentRow> result = tradeMapper.selectPaymentPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    private PlanVersion buildPlan(String code, PlanRequest request, int versionNo) {
        PlanVersion plan = new PlanVersion();
        plan.setCode(code);
        plan.setName(request.name().trim());
        plan.setPriceCents(request.priceCents());
        plan.setDurationDays(value(request.durationDays(), 30));
        plan.setAiImageQuota(value(request.aiImageQuota(), 20));
        plan.setAiVideoQuota(value(request.aiVideoQuota(), 3));
        plan.setBenefitQuota(value(request.benefitQuota(), 2));
        plan.setStatus(value(request.status(), 1));
        plan.setVersionNo(versionNo);
        plan.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return plan;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String newRefundNo() {
        return "RF" + OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
