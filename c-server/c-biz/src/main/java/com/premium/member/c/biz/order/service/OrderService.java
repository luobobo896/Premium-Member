package com.premium.member.c.biz.order.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import com.premium.member.c.biz.member.entity.PmMember;
import com.premium.member.c.biz.member.mapper.PmMemberMapper;
import com.premium.member.c.biz.order.dto.OrderResponse;
import com.premium.member.c.biz.order.dto.PeriodView;
import com.premium.member.c.biz.order.dto.PlanCatalogResponse;
import com.premium.member.c.biz.order.entity.Order;
import com.premium.member.c.biz.order.entity.PlanVersion;
import com.premium.member.c.biz.order.mapper.OrderMapper;
import com.premium.member.c.biz.order.mapper.PlanVersionMapper;
import com.premium.member.c.biz.order.payment.PaymentProvider;
import com.premium.member.c.biz.subscription.service.SubscriptionService;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 套餐下单、模拟支付与周期生成。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter PERIOD_NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PURPOSE = "MEMBER_PLAN";

    private final PlanVersionMapper planMapper;
    private final OrderMapper orderMapper;
    private final PmMemberMapper memberMapper;
    private final List<PaymentProvider> providers;
    private final SubscriptionService subscriptionService;

    public OrderService(PlanVersionMapper planMapper, OrderMapper orderMapper,
            PmMemberMapper memberMapper, List<PaymentProvider> providers,
            SubscriptionService subscriptionService) {
        this.planMapper = planMapper;
        this.orderMapper = orderMapper;
        this.memberMapper = memberMapper;
        this.providers = providers;
        this.subscriptionService = subscriptionService;
    }

    public List<PlanCatalogResponse> plans() {
        return planMapper.selectOnSale().stream().map(PlanCatalogResponse::from).toList();
    }

    @Transactional
    public OrderResponse create(String phone, long userId, long planVersionId, String source) {
        PmMember member = requireActiveMember(phone);
        PlanVersion plan = planMapper.selectOnSaleById(planVersionId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
        if (orderMapper.selectUnfinished(userId, PURPOSE) != null) {
            throw new BusinessException(ErrorCode.ORDER_PENDING_EXISTS);
        }
        refreshPeriods(member.getId());
        if (orderMapper.selectPendingPeriod(member.getId()) != null) {
            throw new BusinessException(ErrorCode.ORDER_PENDING_EXISTS);
        }
        Order order = new Order();
        order.setOrderNo(newOrderNo());
        order.setMemberId(member.getId());
        order.setUserId(userId);
        order.setPlanVersionId(plan.getId());
        order.setAmountCents(plan.getPriceCents());
        order.setStatus("CREATED");
        order.setChannel("MOCK");
        order.setSource(StringUtils.hasText(source) ? source.trim().toUpperCase(Locale.ROOT) : "H5");
        order.setPurpose(PURPOSE);
        order.setRemark("");
        order.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        order.setUpdatedAt(order.getCreatedAt());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.ORDER_PENDING_EXISTS);
        }
        String payMode = provider(order.getChannel()).createPayment(order);
        return OrderResponse.from(order, plan, payMode, null);
    }

    @Transactional
    public List<OrderResponse> list(String phone, long userId) {
        refreshPeriods(requireMember(phone).getId());
        return orderMapper.selectListByUser(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse detail(String phone, long userId, String orderNo) {
        refreshPeriods(requireMember(phone).getId());
        Order order = orderMapper.selectByUserAndNo(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return toResponse(order);
    }

    /**
     * 回调以订单号行锁串行化；交易号唯一 + 订单状态条件更新保证只开通一次。
     */
    @Transactional
    public OrderResponse mockNotify(String orderNo) {
        Order order = orderMapper.lockByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if ("PAID".equals(order.getStatus())) {
            return toResponse(order);
        }
        if (!"CREATED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
        }
        OffsetDateTime paidAt = OffsetDateTime.now(ZoneOffset.UTC);
        String txnNo = "MOCK-" + order.getOrderNo();
        orderMapper.insertSuccessTransaction(txnNo, order.getId(), order.getChannel(),
                order.getAmountCents(), "{\"mock\":true}", paidAt);
        if (orderMapper.markPaid(order.getId(), paidAt) == 1) {
            openPeriod(order, paidAt);
        }
        return toResponse(orderMapper.lockByOrderNo(orderNo));
    }

    private void openPeriod(Order order, OffsetDateTime paidAt) {
        orderMapper.lockMember(order.getMemberId());
        orderMapper.expireEnded(order.getMemberId());
        PlanVersion plan = planMapper.selectById(order.getPlanVersionId());
        int durationDays = plan == null ? 30 : plan.getDurationDays();
        PmMemberPeriod active = orderMapper.selectCurrentPeriod(order.getMemberId());
        PmMemberPeriod period = new PmMemberPeriod();
        period.setMemberId(order.getMemberId());
        period.setOrderId(order.getId());
        period.setPeriodNo(paidAt.format(PERIOD_NO));
        if (active == null) {
            period.setStartAt(paidAt);
            period.setEndAt(paidAt.plusDays(durationDays));
            period.setStatus("ACTIVE");
        } else {
            if (orderMapper.selectPendingPeriod(order.getMemberId()) != null) {
                return;
            }
            period.setStartAt(active.getEndAt());
            period.setEndAt(active.getEndAt().plusDays(durationDays));
            period.setStatus("PENDING");
        }
        orderMapper.insertPeriod(period);
        subscriptionService.activate(order.getMemberId(), order.getPlanVersionId());
    }

    private OrderResponse toResponse(Order order) {
        PlanVersion plan = planMapper.selectById(order.getPlanVersionId());
        PmMemberPeriod period = orderMapper.selectPeriodByOrder(order.getId());
        String payMode = "PAID".equals(order.getStatus()) || "REFUNDING".equals(order.getStatus())
                || "REFUNDED".equals(order.getStatus()) ? null : provider(order.getChannel()).createPayment(order);
        return OrderResponse.from(order, plan, payMode, PeriodView.from(period));
    }

    private PaymentProvider provider(String channel) {
        return providers.stream()
                .filter(item -> item.channel().equals(channel))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_CHANNEL_UNSUPPORTED));
    }

    private PmMember requireActiveMember(String phone) {
        PmMember member = requireMember(phone);
        if (!Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);
        }
        return member;
    }

    private PmMember requireMember(String phone) {
        PmMember member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private void refreshPeriods(long memberId) {
        orderMapper.expireEnded(memberId);
        orderMapper.activateDuePending(memberId);
    }

    private String newOrderNo() {
        return "PM" + OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
