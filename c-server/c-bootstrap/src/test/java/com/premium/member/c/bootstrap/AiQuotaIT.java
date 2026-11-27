package com.premium.member.c.bootstrap;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.premium.member.c.biz.subscription.service.SubscriptionService;
import com.premium.member.c.bootstrap.support.CIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuotaIT extends CIntegrationIT {

    @Autowired
    private SubscriptionService subscriptionService;

    @Test
    void submitConsumesPointsAndFailureRefunds() throws Exception {
        Account account = createAccount("136" + uniqueSuffix(), "ITAI" + uniqueSuffix());
        long planId = createPlan("ITPT" + System.nanoTime(), 0, 0);
        createActivePeriod(account.memberId(), planId);
        subscriptionService.activate(account.memberId(), planId);
        assertThat(balance(account.memberId())).isEqualTo(6000);

        long assetId = createInputAsset(account.memberId());
        long templateId = templateId("AI_PHOTO_01");
        String key = "it-same-key-" + System.nanoTime();
        Map<String, Object> request = Map.of("templateId", templateId,
                "inputAssetId", assetId, "idempotencyKey", key, "mockResult", "FAIL");

        HttpResult first = post("/c/ai/tasks", request, account.token());
        assertThat(first.status()).isEqualTo(200);
        String taskNo = first.data().path("taskNo").asText();
        assertThat(balance(account.memberId())).isEqualTo(5900);

        HttpResult duplicate = post("/c/ai/tasks", request, account.token());
        assertThat(duplicate.status()).isEqualTo(200);
        assertThat(duplicate.data().path("taskNo").asText()).isEqualTo(taskNo);
        assertThat(balance(account.memberId())).isEqualTo(5900);

        awaitStatus(taskNo, account.token(), "FAILED");
        assertThat(balance(account.memberId())).isEqualTo(6000);
        assertThat(ledgerCount(taskNo, "CONSUME")).isEqualTo(1);
        assertThat(ledgerCount(taskNo, "REFUND")).isEqualTo(1);

        String successKey = "it-success-key-" + System.nanoTime();
        HttpResult success = post("/c/ai/tasks", Map.of("templateId", templateId,
                "inputAssetId", assetId, "idempotencyKey", successKey), account.token());
        assertThat(success.status()).isEqualTo(200);
        String successTaskNo = success.data().path("taskNo").asText();
        awaitStatus(successTaskNo, account.token(), "SUCCESS");
        assertThat(balance(account.memberId())).isEqualTo(5900);
        assertThat(ledgerCount(successTaskNo, "CONSUME")).isEqualTo(1);
        assertThat(ledgerCount(successTaskNo, "REFUND")).isZero();

        HttpResult ledger = get("/c/points/ledger", account.token());
        assertThat(ledger.status()).isEqualTo(200);
        assertThat(ledger.data().isArray()).isTrue();
        assertThat(ledger.data().size()).isGreaterThanOrEqualTo(4);

        HttpResult subscription = get("/c/subscription", account.token());
        assertThat(subscription.status()).isEqualTo(200);
        assertThat(subscription.data().path("monthlyPoints").asInt()).isEqualTo(6000);
        assertThat(subscription.data().path("balancePoints").asInt()).isEqualTo(5900);

        Account other = createAccount("135" + uniqueSuffix(), "ITAI2" + uniqueSuffix());
        HttpResult conflict = post("/c/ai/tasks", request, other.token());
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.bizCode()).isEqualTo("AI_IDEMPOTENCY_CONFLICT");
    }

    @Test
    void insufficientPointsRejectsCreate() {
        Account account = createAccount("134" + uniqueSuffix(), "ITAI0" + uniqueSuffix());
        long planId = createPlan("ITZERO" + System.nanoTime(), 0, 0);
        jdbc.update("UPDATE plan_version SET monthly_points = 0 WHERE id = ?", planId);
        createActivePeriod(account.memberId(), planId);
        subscriptionService.activate(account.memberId(), planId);
        long assetId = createInputAsset(account.memberId());
        String key = "it-zero-key-" + System.nanoTime();

        HttpResult result = post("/c/ai/tasks", Map.of("templateId", templateId("AI_PHOTO_01"),
                "inputAssetId", assetId, "idempotencyKey", key), account.token());
        assertThat(result.status()).isEqualTo(409);
        assertThat(result.bizCode()).isEqualTo("POINTS_INSUFFICIENT");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM ai_task WHERE idempotency_key = ?", Long.class, key))
                .isZero();
    }

    private int balance(long memberId) {
        return jdbc.queryForObject(
                "SELECT balance_points FROM point_account WHERE member_id = ?",
                Integer.class, memberId);
    }

    private long ledgerCount(String taskNo, String type) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM point_ledger
                WHERE ref_type = 'AI_TASK' AND ref_id = ? AND type = ?
                """, Long.class, taskNo, type);
    }

    private void awaitStatus(String taskNo, String token, String expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(deadline)) {
            HttpResult detail = get("/c/ai/tasks/" + taskNo, token);
            assertThat(detail.status()).isEqualTo(200);
            String status = detail.data().path("status").asText();
            if (expected.equals(status)) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("task " + taskNo + " did not reach " + expected);
    }
}
