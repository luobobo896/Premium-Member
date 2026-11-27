package com.premium.member.c.bootstrap;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.premium.member.c.biz.ai.service.AiQuotaService;
import com.premium.member.c.bootstrap.support.CIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuotaIT extends CIntegrationIT {

    @Autowired
    private AiQuotaService quotaService;

    @Test
    void reserveSettleReleaseAndIdempotency() throws Exception {
        Account account = createAccount("136" + suffix(), "ITAI" + System.nanoTime());
        long periodId = createActivePeriod(account.memberId(), 1);

        quotaService.reserve(periodId, "IMAGE", 0, "IT-RESERVE-" + periodId, "IT");
        assertThat(sumDelta(periodId, "IMAGE")).isEqualTo(-1);
        quotaService.reserve(periodId, "IMAGE", 0, "IT-RESERVE2-" + periodId, "IT");
        assertThat(sumDelta(periodId, "IMAGE")).isEqualTo(-2);
        quotaService.settle(periodId, "IMAGE", 0, "IT-RESERVE2-" + periodId);
        assertThat(sumDelta(periodId, "IMAGE")).isEqualTo(-2);
        quotaService.release(periodId, "IMAGE", 0, "IT-RESERVE2-" + periodId);
        assertThat(sumDelta(periodId, "IMAGE")).isEqualTo(-1);
        assertThat(ledgerCount(periodId, "IMAGE", "RESERVE")).isEqualTo(2);
        assertThat(ledgerCount(periodId, "IMAGE", "SETTLE")).isEqualTo(1);
        assertThat(ledgerCount(periodId, "IMAGE", "RELEASE")).isEqualTo(1);

        long assetId = createInputAsset(account.memberId());
        long templateId = templateId("AI_PHOTO_01");
        String key = "it-same-key-" + System.nanoTime();
        Map<String, Object> request = Map.of("templateId", templateId,
                "inputAssetId", assetId, "idempotencyKey", key, "mockResult", "FAIL");

        HttpResult first = post("/c/ai/tasks", request, account.token());
        assertThat(first.status()).isEqualTo(200);
        String taskNo = first.data().path("taskNo").asText();
        HttpResult duplicate = post("/c/ai/tasks", request, account.token());
        assertThat(duplicate.status()).isEqualTo(200);
        assertThat(duplicate.data().path("taskNo").asText()).isEqualTo(taskNo);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM ai_task WHERE idempotency_key = ?", Long.class, key))
                .isEqualTo(1);

        awaitStatus(taskNo, account.token(), "FAILED");
        assertThat(ledgerCountForTask(taskNo, "RESERVE")).isEqualTo(1);
        assertThat(ledgerCountForTask(taskNo, "RELEASE")).isEqualTo(1);

        String successKey = "it-success-key-" + System.nanoTime();
        HttpResult success = post("/c/ai/tasks", Map.of("templateId", templateId,
                "inputAssetId", assetId, "idempotencyKey", successKey), account.token());
        assertThat(success.status()).isEqualTo(200);
        String successTaskNo = success.data().path("taskNo").asText();
        awaitStatus(successTaskNo, account.token(), "SUCCESS");
        assertThat(ledgerCountForTask(successTaskNo, "RESERVE")).isEqualTo(1);
        assertThat(ledgerCountForTask(successTaskNo, "SETTLE")).isEqualTo(1);

        Account other = createAccount("135" + suffix(), "ITAI2" + System.nanoTime());
        HttpResult conflict = post("/c/ai/tasks", request, other.token());
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.bizCode()).isEqualTo("AI_IDEMPOTENCY_CONFLICT");
    }

    @Test
    void exhaustedQuotaRejectsCreate() {
        Account account = createAccount("134" + suffix(), "ITAI0" + System.nanoTime());
        long planId = createPlan("ITZERO" + System.nanoTime(), 0, 0);
        createActivePeriod(account.memberId(), planId);
        long assetId = createInputAsset(account.memberId());
        String key = "it-zero-key-" + System.nanoTime();

        HttpResult result = post("/c/ai/tasks", Map.of("templateId", templateId("AI_PHOTO_01"),
                "inputAssetId", assetId, "idempotencyKey", key), account.token());
        assertThat(result.status()).isEqualTo(409);
        assertThat(result.bizCode()).isEqualTo("AI_QUOTA_EXHAUSTED");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM ai_task WHERE idempotency_key = ?", Long.class, key))
                .isZero();
    }

    private int sumDelta(long periodId, String kind) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(delta), 0) FROM quota_ledger
                WHERE period_id = ? AND kind = ?
                """, Integer.class, periodId, kind);
    }

    private long ledgerCount(long periodId, String kind, String reason) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM quota_ledger
                WHERE period_id = ? AND kind = ? AND reason = ?
                """, Long.class, periodId, kind, reason);
    }

    private long ledgerCountForTask(String taskNo, String reason) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM quota_ledger q
                JOIN ai_task t ON t.id = q.ref_task_id
                WHERE t.task_no = ? AND q.reason = ?
                """, Long.class, taskNo, reason);
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

    private String suffix() {
        return String.valueOf(System.nanoTime()).substring(6, 14);
    }
}
