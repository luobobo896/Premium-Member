package com.premium.member.c.bootstrap;

import java.util.Map;

import com.premium.member.c.bootstrap.support.CIntegrationIT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPaymentIT extends CIntegrationIT {

    @Test
    void duplicateOrderAndDuplicateCallbackAreIdempotent() {
        Account account = createAccount("138" + uniqueSuffix(), "ITORDER" + uniqueSuffix());
        Map<String, Object> request = Map.of("planVersionId", 1, "source", "H5");

        HttpResult created = post("/c/orders", request, account.token());
        assertThat(created.status()).isEqualTo(200);
        assertThat(created.data().path("status").asText()).isEqualTo("CREATED");
        String orderNo = created.data().path("orderNo").asText();
        assertThat(orderNo).isNotBlank();

        HttpResult duplicate = post("/c/orders", request, account.token());
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.bizCode()).isEqualTo("ORDER_PENDING_EXISTS");

        HttpResult firstNotify = post("/c/payments/mock/notify",
                Map.of("orderNo", orderNo), null);
        assertThat(firstNotify.status()).isEqualTo(200);
        assertThat(firstNotify.data().path("status").asText()).isEqualTo("PAID");

        HttpResult secondNotify = post("/c/payments/mock/notify",
                Map.of("orderNo", orderNo), null);
        assertThat(secondNotify.status()).isEqualTo(200);
        assertThat(secondNotify.data().path("status").asText()).isEqualTo("PAID");

        Long orderId = jdbc.queryForObject(
                "SELECT id FROM orders WHERE order_no = ?", Long.class, orderNo);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM payment_transactions WHERE order_id = ? AND status = 'SUCCESS'",
                Long.class, orderId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM pm_member_period WHERE order_id = ?",
                Long.class, orderId)).isEqualTo(1);

        HttpResult detail = get("/c/orders/" + orderNo, account.token());
        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.data().path("status").asText()).isEqualTo("PAID");
    }
}
