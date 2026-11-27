package com.premium.member.bootstrap;

import java.util.Map;

import com.premium.member.bootstrap.support.AdminIntegrationIT;
import com.premium.member.bootstrap.support.AdminIntegrationIT.HttpResult;
import com.premium.member.bootstrap.support.AdminIntegrationIT.Login;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class TradeRefundIT extends AdminIntegrationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void refundCannotExceedPaidAmount() {
        String orderNo = "PMIT" + System.nanoTime();
        Long orderId = jdbc.queryForObject("""
                INSERT INTO orders (order_no, member_id, user_id, plan_version_id, amount_cents,
                                    status, channel, source, purpose, remark, created_at, updated_at, paid_at)
                VALUES (?, 1, 1, 1, 5900, 'PAID', 'MOCK', 'H5', 'MEMBER_PLAN', '',
                        now(), now(), now())
                RETURNING id
                """, Long.class, orderNo);

        Login admin = login("admin", "Admin@123456");
        HttpResult overpay = post("/system/orders/" + orderNo + "/refund",
                Map.of("amountCents", 5901, "reason", "test overpay", "result", "APPROVE"),
                admin.token(), null);
        assertThat(overpay.status()).isEqualTo(409);
        assertThat(overpay.bizCode()).isEqualTo("REFUND_AMOUNT_EXCEEDS");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refunds WHERE order_id = ?",
                Long.class, orderId)).isZero();

        HttpResult approved = post("/system/orders/" + orderNo + "/refund",
                Map.of("amountCents", 5900, "reason", "test refund", "result", "APPROVE"),
                admin.token(), null);
        assertThat(approved.status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?",
                String.class, orderId)).isEqualTo("REFUNDED");
        assertThat(jdbc.queryForObject("SELECT status FROM refunds WHERE order_id = ?",
                String.class, orderId)).isEqualTo("SUCCESS");
    }
}
