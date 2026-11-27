package com.premium.member.c.biz.subscription.dto;

import java.time.OffsetDateTime;

import com.premium.member.c.biz.subscription.entity.PointLedger;

public record PointLedgerView(
        long id,
        int delta,
        int balanceAfter,
        String type,
        String refType,
        String refId,
        String period,
        String operator,
        OffsetDateTime createdAt) {

    public static PointLedgerView from(PointLedger ledger) {
        return new PointLedgerView(ledger.getId(), ledger.getDelta(), ledger.getBalanceAfter(),
                ledger.getType(), ledger.getRefType(), ledger.getRefId(), ledger.getPeriod(),
                ledger.getOperator(), ledger.getCreatedAt());
    }
}
