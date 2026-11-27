package com.premium.member.c.biz.subscription.service;

import java.time.YearMonth;
import java.time.ZoneId;

import com.premium.member.c.biz.subscription.entity.PointAccount;
import com.premium.member.c.biz.subscription.mapper.SubscriptionMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分账户与流水。所有余额变更都先锁账户，再追加一条带幂等键的账本。
 */
@Service
public class PointService {

    public static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Shanghai");

    private final SubscriptionMapper mapper;

    public PointService(SubscriptionMapper mapper) {
        this.mapper = mapper;
    }

    public int balance(long memberId) {
        PointAccount account = mapper.selectAccount(memberId);
        return account == null ? 0 : account.getBalancePoints();
    }

    @Transactional
    public int grant(long memberId, long subscriptionId, int points, String period,
            String operator) {
        String key = "GRANT:" + subscriptionId + ":" + period;
        if (mapper.selectLedgerByKey(key) != null) {
            return balance(memberId);
        }
        return change(memberId, points, "GRANT", "SUBSCRIPTION",
                String.valueOf(subscriptionId), period, key, operator);
    }

    @Transactional
    public int reset(long memberId, long subscriptionId, String period, String operator) {
        String key = "RESET:" + subscriptionId + ":" + period;
        if (mapper.selectLedgerByKey(key) != null) {
            return balance(memberId);
        }
        PointAccount account = lock(memberId);
        int balance = account.getBalancePoints();
        mapper.addBalance(memberId, -balance);
        mapper.insertLedger(memberId, -balance, 0, "RESET", "SUBSCRIPTION",
                String.valueOf(subscriptionId), period, key, operator);
        return 0;
    }

    @Transactional
    public void consume(long memberId, int points, String taskNo, String period) {
        String key = "CONSUME:" + taskNo;
        if (mapper.selectLedgerByKey(key) != null) {
            return;
        }
        PointAccount account = lock(memberId);
        if (account.getBalancePoints() < points) {
            throw new BusinessException(ErrorCode.POINTS_INSUFFICIENT);
        }
        int after = account.getBalancePoints() - points;
        mapper.addBalance(memberId, -points);
        mapper.insertLedger(memberId, -points, after, "CONSUME", "AI_TASK", taskNo, period,
                key, "");
    }

    @Transactional
    public void refund(long memberId, int points, String taskNo, String period) {
        String key = "REFUND:" + taskNo;
        if (mapper.selectLedgerByKey(key) != null) {
            return;
        }
        change(memberId, points, "REFUND", "AI_TASK", taskNo, period, key, "SYSTEM");
    }

    public static String currentPeriod() {
        return YearMonth.now(BILLING_ZONE).toString();
    }

    private int change(long memberId, int delta, String type, String refType, String refId,
            String period, String key, String operator) {
        PointAccount account = lock(memberId);
        int after = account.getBalancePoints() + delta;
        if (after < 0) {
            throw new BusinessException(ErrorCode.POINTS_INSUFFICIENT);
        }
        mapper.addBalance(memberId, delta);
        mapper.insertLedger(memberId, delta, after, type, refType, refId, period, key, operator);
        return after;
    }

    private PointAccount lock(long memberId) {
        mapper.insertAccountIfAbsent(memberId);
        PointAccount account = mapper.lockAccount(memberId);
        if (account == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return account;
    }
}
