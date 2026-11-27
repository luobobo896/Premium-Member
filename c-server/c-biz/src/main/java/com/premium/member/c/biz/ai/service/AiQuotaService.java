package com.premium.member.c.biz.ai.service;

import com.premium.member.c.biz.ai.dto.QuotaLimit;
import com.premium.member.c.biz.ai.dto.QuotaResponse;
import com.premium.member.c.biz.ai.mapper.AiMapper;
import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import com.premium.member.c.biz.order.dto.PeriodView;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * 周期额度账本：RESERVE 预占，SETTLE 确认，RELEASE 返还。
 */
@Service
public class AiQuotaService {

    private final AiMapper aiMapper;

    public AiQuotaService(AiMapper aiMapper) {
        this.aiMapper = aiMapper;
    }

    public QuotaResponse forPeriod(PmMemberPeriod period) {
        if (period == null) {
            return new QuotaResponse(null, new QuotaResponse.QuotaPart(0, 0, 0),
                    new QuotaResponse.QuotaPart(0, 0, 0));
        }
        QuotaLimit limit = limit(period.getId());
        int imageRemaining = remaining(limit.getImageLimit(), period.getId(), "IMAGE");
        int videoRemaining = remaining(limit.getVideoLimit(), period.getId(), "VIDEO");
        return new QuotaResponse(PeriodView.from(period),
                part(limit.getImageLimit(), imageRemaining),
                part(limit.getVideoLimit(), videoRemaining));
    }

    public int reserve(long periodId, String kind, long taskId, String taskNo, String operator) {
        aiMapper.lockPeriod(periodId);
        int limit = limitValue(periodId, kind);
        int balance = limit + aiMapper.sumDelta(periodId, kind);
        if (balance <= 0) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXHAUSTED);
        }
        aiMapper.insertLedger(periodId, kind, -1, balance - 1, "RESERVE", taskId,
                "RESERVE:" + taskNo, operator);
        return balance - 1;
    }

    public void settle(long periodId, String kind, long taskId, String taskNo) {
        aiMapper.lockPeriod(periodId);
        int limit = limitValue(periodId, kind);
        int balance = limit + aiMapper.sumDelta(periodId, kind);
        aiMapper.insertLedger(periodId, kind, 0, Math.max(0, balance), "SETTLE", taskId,
                "SETTLE:" + taskNo, "SYSTEM");
    }

    public void release(long periodId, String kind, long taskId, String taskNo) {
        aiMapper.lockPeriod(periodId);
        int limit = limitValue(periodId, kind);
        int balance = limit + aiMapper.sumDelta(periodId, kind);
        int after = Math.min(limit, balance + 1);
        aiMapper.insertLedger(periodId, kind, after - balance, after, "RELEASE", taskId,
                "RELEASE:" + taskNo, "SYSTEM");
    }

    private QuotaResponse.QuotaPart part(int limit, int remaining) {
        return new QuotaResponse.QuotaPart(limit, Math.max(0, limit - remaining),
                Math.max(0, remaining));
    }

    private int remaining(int limit, long periodId, String kind) {
        return Math.max(0, Math.min(limit, limit + aiMapper.sumDelta(periodId, kind)));
    }

    private int limitValue(long periodId, String kind) {
        QuotaLimit limit = limit(periodId);
        return "IMAGE".equals(kind) ? limit.getImageLimit() : limit.getVideoLimit();
    }

    private QuotaLimit limit(long periodId) {
        QuotaLimit limit = aiMapper.selectQuotaLimit(periodId);
        if (limit == null || limit.getImageLimit() == null || limit.getVideoLimit() == null) {
            return QuotaLimit.defaults();
        }
        return limit;
    }
}
