package com.premium.member.member.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.member.entity.ChargeRecord;
import com.premium.member.member.entity.Subscription;
import com.premium.member.member.mapper.SubscriptionMapper;
import com.premium.member.member.provider.WithholdProvider;
import com.premium.member.member.provider.WithholdResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单订阅的续费、宽限重试和积分账本。每次调用都是一个独立事务。
 */
@Service
public class SubscriptionBillingService {

    public enum Outcome {
        SKIPPED, GRANTED, FAILED, SUSPENDED
    }

    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime CHARGE_TIME = LocalTime.of(0, 5);

    private final SubscriptionMapper mapper;
    private final List<WithholdProvider> providers;
    private final SubscriptionNotifier notifier;
    private final String providerName;
    private final int defaultGraceDays;

    public SubscriptionBillingService(SubscriptionMapper mapper, List<WithholdProvider> providers,
            SubscriptionNotifier notifier,
            @Value("${app.subscription.provider:mock}") String providerName,
            @Value("${app.subscription.grace-days:3}") int defaultGraceDays) {
        this.mapper = mapper;
        this.providers = providers;
        this.notifier = notifier;
        this.providerName = providerName;
        this.defaultGraceDays = defaultGraceDays;
    }

    @Transactional
    public Outcome renew(long subscriptionId, String period, String mockResult,
            Integer graceDays, boolean ignoreDue) {
        Subscription subscription = mapper.lockById(subscriptionId);
        if (subscription == null || "CANCELLED".equals(subscription.getStatus())
                || "SUSPENDED".equals(subscription.getStatus())) {
            return Outcome.SKIPPED;
        }
        OffsetDateTime now = OffsetDateTime.now(BILLING_ZONE);
        if (!ignoreDue && subscription.getNextChargeAt() != null
                && subscription.getNextChargeAt().isAfter(now)) {
            return Outcome.SKIPPED;
        }
        String billingPeriod = resolvePeriod(period, subscription.getNextChargeAt());
        if (mapper.selectLedgerKey("GRANT:" + subscriptionId + ":" + billingPeriod) != null) {
            return Outcome.SKIPPED;
        }
        resetPoints(subscription, billingPeriod);
        int attempt = mapper.maxAttempt(subscriptionId, billingPeriod) + 1;
        ChargeRecord charge = newCharge(subscription, billingPeriod, attempt);
        if (mapper.insertCharge(charge) != 1) {
            return Outcome.SKIPPED;
        }
        return charge(subscription, charge, billingPeriod, mockResult,
                graceDays == null ? defaultGraceDays : graceDays);
    }

    @Transactional
    public Outcome retryOrSuspend(long subscriptionId, String mockResult, boolean forceSuspend) {
        Subscription subscription = mapper.lockById(subscriptionId);
        if (subscription == null || !"PAST_DUE".equals(subscription.getStatus())) {
            return Outcome.SKIPPED;
        }
        OffsetDateTime now = OffsetDateTime.now(BILLING_ZONE);
        if (forceSuspend || (subscription.getGraceUntil() != null
                && subscription.getGraceUntil().isBefore(now))) {
            mapper.markSuspended(subscriptionId, subscription.getFailureReason());
            notifier.suspended(subscription, subscription.getFailureReason());
            return Outcome.SUSPENDED;
        }
        String billingPeriod = resolvePeriod(null, subscription.getNextChargeAt());
        if (mapper.selectLedgerKey("GRANT:" + subscriptionId + ":" + billingPeriod) != null) {
            return Outcome.SKIPPED;
        }
        int attempt = mapper.maxAttempt(subscriptionId, billingPeriod) + 1;
        ChargeRecord charge = newCharge(subscription, billingPeriod, attempt);
        if (mapper.insertCharge(charge) != 1) {
            return Outcome.SKIPPED;
        }
        return charge(subscription, charge, billingPeriod, mockResult, null);
    }

    @Transactional
    public void grantManual(long subscriptionId, int points, String reason, String operator) {
        Subscription subscription = mapper.lockById(subscriptionId);
        if (subscription == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订阅不存在");
        }
        mapper.insertAccountIfAbsent(subscription.getMemberId());
        Integer balance = mapper.lockBalance(subscription.getMemberId());
        int after = (balance == null ? 0 : balance) + points;
        mapper.addBalance(subscription.getMemberId(), points);
        String refId = reason.length() > 60 ? reason.substring(0, 60) : reason;
        mapper.insertLedger(subscription.getMemberId(), points, after, "ADJUST", "MANUAL",
                refId, YearMonth.now(BILLING_ZONE).toString(),
                "ADJUST:" + subscriptionId + ":" + UUID.randomUUID(), operator);
    }

    private Outcome charge(Subscription subscription, ChargeRecord charge, String period,
            String mockResult, Integer graceDays) {
        WithholdResult result = provider().withhold(subscription, period,
                charge.getAmountCents(), mockResult);
        OffsetDateTime now = OffsetDateTime.now(BILLING_ZONE);
        if (!result.success()) {
            String reason = truncate(result.reason());
            mapper.markChargeFailed(charge.getId(), reason);
            OffsetDateTime graceUntil = graceDays == null
                    ? subscription.getGraceUntil() : now.plusDays(graceDays);
            if (graceDays != null && graceDays <= 0) {
                mapper.markSuspended(subscription.getId(), reason);
                notifier.suspended(subscription, reason);
                return Outcome.SUSPENDED;
            }
            if (graceUntil == null || !graceUntil.isAfter(now)) {
                mapper.markSuspended(subscription.getId(), reason);
                notifier.suspended(subscription, reason);
                return Outcome.SUSPENDED;
            }
            mapper.markPastDue(subscription.getId(), graceUntil, now, reason);
            return Outcome.FAILED;
        }
        mapper.markChargeSuccess(charge.getId(), result.txnNo());
        grant(subscription, period);
        mapper.markActive(subscription.getId(), nextCharge(period), now);
        return Outcome.GRANTED;
    }

    private void resetPoints(Subscription subscription, String period) {
        String key = "RESET:" + subscription.getId() + ":" + period;
        if (mapper.selectLedgerKey(key) != null) {
            return;
        }
        mapper.insertAccountIfAbsent(subscription.getMemberId());
        Integer balance = mapper.lockBalance(subscription.getMemberId());
        int current = balance == null ? 0 : balance;
        mapper.addBalance(subscription.getMemberId(), -current);
        mapper.insertLedger(subscription.getMemberId(), -current, 0, "RESET", "SUBSCRIPTION",
                String.valueOf(subscription.getId()), period, key, "SYSTEM");
    }

    private void grant(Subscription subscription, String period) {
        String key = "GRANT:" + subscription.getId() + ":" + period;
        if (mapper.selectLedgerKey(key) != null) {
            return;
        }
        mapper.insertAccountIfAbsent(subscription.getMemberId());
        Integer balance = mapper.lockBalance(subscription.getMemberId());
        int after = (balance == null ? 0 : balance) + subscription.getMonthlyPoints();
        mapper.addBalance(subscription.getMemberId(), subscription.getMonthlyPoints());
        mapper.insertLedger(subscription.getMemberId(), subscription.getMonthlyPoints(), after,
                "GRANT", "SUBSCRIPTION", String.valueOf(subscription.getId()), period, key,
                "SYSTEM");
    }

    private ChargeRecord newCharge(Subscription subscription, String period, int attempt) {
        ChargeRecord record = new ChargeRecord();
        record.setChargeNo("CH" + subscription.getId() + period.replace("-", "")
                + "-" + attempt + "-" + UUID.randomUUID().toString().substring(0, 6));
        record.setSubscriptionId(subscription.getId());
        record.setMemberId(subscription.getMemberId());
        record.setPeriod(period);
        record.setAmountCents(subscription.getPriceCents());
        record.setStatus("PENDING");
        record.setProvider(provider().name());
        record.setChannelTxnNo("");
        record.setFailReason("");
        record.setAttemptNo(attempt);
        return record;
    }

    private WithholdProvider provider() {
        return providers.stream()
                .filter(item -> item.name().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "unknown withhold provider: " + providerName));
    }

    private String resolvePeriod(String period, OffsetDateTime nextChargeAt) {
        if (period != null && !period.isBlank()) {
            return period;
        }
        if (nextChargeAt == null) {
            return YearMonth.now(BILLING_ZONE).toString();
        }
        return YearMonth.from(nextChargeAt.atZoneSameInstant(BILLING_ZONE)).toString();
    }

    private OffsetDateTime nextCharge(String period) {
        YearMonth month = YearMonth.parse(period).plusMonths(1);
        LocalDate date = month.atDay(1);
        return ZonedDateTime.of(date, CHARGE_TIME, BILLING_ZONE).toOffsetDateTime();
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.length() > 255 ? reason.substring(0, 255) : reason;
    }
}
