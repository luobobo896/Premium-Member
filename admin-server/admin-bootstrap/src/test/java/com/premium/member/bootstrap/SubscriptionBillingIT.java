package com.premium.member.bootstrap;

import java.util.Map;

import com.premium.member.bootstrap.support.AdminIntegrationIT;
import com.premium.member.bootstrap.support.AdminIntegrationIT.HttpResult;
import com.premium.member.bootstrap.support.AdminIntegrationIT.Login;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionBillingIT extends AdminIntegrationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void billingResetsGrantsAndIsIdempotent() {
        Login admin = login("admin", "Admin@123456");
        int before = balance(1);
        assertThat(before).isEqualTo(6000);

        HttpResult first = post("/system/subscriptions/run-billing",
                Map.of("subscriptionId", 1, "mockResult", "SUCCESS", "period", "2026-12"),
                admin.token(), null);
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.json().path("data").path("granted").asInt()).isEqualTo(1);
        assertThat(balance(1)).isEqualTo(6000);
        assertThat(ledgerCount(1, "2026-12", "RESET")).isEqualTo(1);
        assertThat(ledgerCount(1, "2026-12", "GRANT")).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM charge_record WHERE subscription_id = 1 AND period = '2026-12'",
                String.class)).isEqualTo("SUCCESS");

        HttpResult second = post("/system/subscriptions/run-billing",
                Map.of("subscriptionId", 1, "mockResult", "SUCCESS", "period", "2026-12"),
                admin.token(), null);
        assertThat(second.status()).isEqualTo(200);
        assertThat(second.json().path("data").path("granted").asInt()).isZero();
        assertThat(balance(1)).isEqualTo(6000);
        assertThat(ledgerCount(1, "2026-12", "GRANT")).isEqualTo(1);

        HttpResult grant = post("/system/subscriptions/1/grant-points",
                Map.of("points", 500, "reason", "integration compensation"),
                admin.token(), null);
        assertThat(grant.status()).isEqualTo(200);
        assertThat(balance(1)).isEqualTo(6500);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM point_ledger
                WHERE member_id = 1 AND type = 'ADJUST' AND ref_type = 'MANUAL'
                """, Long.class)).isEqualTo(1);
    }

    @Test
    void failedChargeMovesToPastDueThenSuspendsWithoutGrant() {
        long memberId = jdbc.queryForObject("""
                INSERT INTO pm_member (member_no, username, owner_id, nickname, phone, level,
                                       points, balance, status, joined_at, remark)
                VALUES (?, ?, 1, '订阅失败样例', ?, 'BASIC', 0, 0, 1, now(), 'it')
                RETURNING id
                """, Long.class, "ITSUB" + System.nanoTime(), "itsub" + System.nanoTime(),
                "137" + uniquePhone());
        long subscriptionId = jdbc.queryForObject("""
                INSERT INTO subscription (member_id, plan_id, status, monthly_points, price_cents,
                                          billing_day, next_charge_at, last_charge_at, version)
                VALUES (?, 1, 'ACTIVE', 6000, 9900, 1, now() - INTERVAL '1 hour', NULL, 0)
                RETURNING id
                """, Long.class, memberId);
        jdbc.update("INSERT INTO point_account (member_id, balance_points) VALUES (?, 6000)",
                memberId);
        jdbc.update("""
                INSERT INTO point_ledger (member_id, delta, balance_after, type, ref_type, ref_id,
                                          period, idempotency_key, operator)
                VALUES (?, 6000, 6000, 'GRANT', 'SEED', 'it', '2026-11', ?, 'TEST')
                """, memberId, "GRANT:SEED:" + subscriptionId);

        Login admin = login("admin", "Admin@123456");
        HttpResult failed = post("/system/subscriptions/run-billing",
                Map.of("subscriptionId", subscriptionId, "mockResult", "FAIL", "graceDays", 3,
                        "period", "2026-11"),
                admin.token(), null);
        assertThat(failed.status()).isEqualTo(200);
        assertThat(failed.json().path("data").path("failed").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM subscription WHERE id = ?", String.class, subscriptionId))
                .isEqualTo("PAST_DUE");
        assertThat(balance(memberId)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM point_ledger WHERE member_id = ? AND type = 'GRANT'
                """, Long.class, memberId)).isEqualTo(1);

        HttpResult suspended = post("/system/subscriptions/run-dunning",
                Map.of("subscriptionId", subscriptionId, "forceSuspend", true),
                admin.token(), null);
        assertThat(suspended.status()).isEqualTo(200);
        assertThat(suspended.json().path("data").path("suspended").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM subscription WHERE id = ?", String.class, subscriptionId))
                .isEqualTo("SUSPENDED");
        assertThat(balance(memberId)).isZero();
    }

    private int balance(long memberId) {
        return jdbc.queryForObject(
                "SELECT balance_points FROM point_account WHERE member_id = ?",
                Integer.class, memberId);
    }

    private long ledgerCount(long memberId, String period, String type) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM point_ledger
                WHERE member_id = ? AND period = ? AND type = ?
                """, Long.class, memberId, period, type);
    }

    private String uniquePhone() {
        return String.valueOf(System.nanoTime()).substring(7);
    }
}
