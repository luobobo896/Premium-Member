package com.premium.member.c.biz.subscription.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import com.premium.member.c.biz.member.entity.PmMember;
import com.premium.member.c.biz.member.mapper.PmMemberMapper;
import com.premium.member.c.biz.subscription.dto.PointLedgerView;
import com.premium.member.c.biz.subscription.dto.SubscriptionView;
import com.premium.member.c.biz.subscription.entity.Subscription;
import com.premium.member.c.biz.subscription.mapper.SubscriptionMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * C 端订阅视图、首次开通与积分明细。
 */
@Service
public class SubscriptionService {

    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final DateTimeFormatter STATUS_DATE = DateTimeFormatter.ofPattern("M-d");
    private static final LocalTime CHARGE_TIME = LocalTime.of(0, 5);

    private final SubscriptionMapper mapper;
    private final PmMemberMapper memberMapper;
    private final PointService pointService;

    public SubscriptionService(SubscriptionMapper mapper, PmMemberMapper memberMapper,
            PointService pointService) {
        this.mapper = mapper;
        this.memberMapper = memberMapper;
        this.pointService = pointService;
    }

    public SubscriptionView view(String phone) {
        PmMember member = requireMember(phone);
        Subscription subscription = mapper.selectByMember(member.getId());
        if (subscription == null) {
            return null;
        }
        return toView(member, subscription);
    }

    public List<PointLedgerView> ledger(String phone, String month) {
        PmMember member = requireMember(phone);
        String normalized = StringUtils.hasText(month) ? month.trim() : "";
        if (!normalized.isEmpty() && !MONTH.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month 格式为 YYYY-MM");
        }
        return mapper.selectLedger(member.getId(), normalized).stream()
                .map(PointLedgerView::from)
                .toList();
    }

    /**
     * 首次支付成功开通订阅，并发放当月积分。后续月份由 admin 侧 1 号任务发放。
     */
    @Transactional
    public void activate(long memberId, long planId) {
        Integer monthlyPoints = mapper.selectPlanMonthlyPoints(planId);
        Long priceCents = mapper.selectPlanPriceCents(planId);
        OffsetDateTime now = OffsetDateTime.now(PointService.BILLING_ZONE);
        Subscription subscription = new Subscription();
        subscription.setMemberId(memberId);
        subscription.setPlanId(planId);
        subscription.setStatus("ACTIVE");
        subscription.setMonthlyPoints(monthlyPoints == null ? 0 : monthlyPoints);
        subscription.setPriceCents(priceCents == null ? 0 : priceCents);
        subscription.setBillingDay(1);
        subscription.setNextChargeAt(nextMonthFirstCharge(now));
        subscription.setLastChargeAt(now);
        subscription.setSignContractNo("");
        if (mapper.insert(subscription) != 1) {
            return;
        }
        pointService.grant(memberId, subscription.getId(), subscription.getMonthlyPoints(),
                PointService.currentPeriod(), "SYSTEM");
    }

    private SubscriptionView toView(PmMember member, Subscription subscription) {
        int balance = pointService.balance(member.getId());
        return new SubscriptionView(subscription.getId(), subscription.getPlanId(),
                mapper.selectPlanName(subscription.getPlanId()), member.getLevel(),
                member.getMemberNo(), subscription.getStatus(),
                statusText(subscription), subscription.getMonthlyPoints(),
                subscription.getPriceCents(), balance, PointService.currentPeriod(),
                subscription.getNextChargeAt(), subscription.getLastChargeAt(),
                subscription.getGraceUntil(), subscription.getFailureReason());
    }

    private String statusText(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case "ACTIVE" -> subscription.getNextChargeAt() != null
                    && !subscription.getNextChargeAt().isAfter(OffsetDateTime.now())
                    ? "发放中" : "正常";
            case "PAST_DUE" -> subscription.getGraceUntil() == null
                    ? "待扣款" : "待扣款（宽限至 " + subscription.getGraceUntil()
                    .format(STATUS_DATE) + "）";
            case "SUSPENDED" -> "已停用";
            case "CANCELLED" -> "已取消";
            default -> subscription.getStatus();
        };
    }

    private OffsetDateTime nextMonthFirstCharge(OffsetDateTime now) {
        LocalDate nextMonth = now.toLocalDate().withDayOfMonth(1).plusMonths(1);
        return ZonedDateTime.of(nextMonth, CHARGE_TIME, PointService.BILLING_ZONE)
                .toOffsetDateTime();
    }

    private PmMember requireMember(String phone) {
        PmMember member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }
}
