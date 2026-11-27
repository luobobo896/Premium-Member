package com.premium.member.c.bootstrap;

import com.premium.member.c.bootstrap.support.CIntegrationIT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitClaimIT extends CIntegrationIT {

    @Test
    void quotaSameSkuAndCardStockRules() {
        Account account = createAccount("137" + suffix(), "ITBENEFIT" + System.nanoTime());
        long periodId = createActivePeriod(account.memberId(), 1);
        insertCard("COFFEE_30", "COFFEE-IT-001");

        HttpResult first = post("/c/benefits/COFFEE_30/claim", null, account.token());
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.data().path("status").asText()).isEqualTo("SUCCESS");
        assertThat(first.data().path("alreadyClaimed").asBoolean()).isFalse();
        assertThat(first.data().path("cardCode").asText()).isEqualTo("COFFEE-IT-001");
        long claimId = first.data().path("claimId").asLong();

        HttpResult sameSku = post("/c/benefits/COFFEE_30/claim", null, account.token());
        assertThat(sameSku.status()).isEqualTo(200);
        assertThat(sameSku.data().path("alreadyClaimed").asBoolean()).isTrue();
        assertThat(sameSku.data().path("claimId").asLong()).isEqualTo(claimId);
        assertThat(sameSku.data().path("cardCode").isNull()).isTrue();

        HttpResult outOfStock = post("/c/benefits/MOVIE_1/claim", null, account.token());
        assertThat(outOfStock.status()).isEqualTo(409);
        assertThat(outOfStock.bizCode()).isEqualTo("BENEFIT_CARD_OUT_OF_STOCK");

        insertCard("MOVIE_1", "MOVIE-IT-001");
        HttpResult secondSku = post("/c/benefits/MOVIE_1/claim", null, account.token());
        assertThat(secondSku.status()).isEqualTo(200);
        assertThat(secondSku.data().path("status").asText()).isEqualTo("SUCCESS");

        HttpResult quotaExhausted = post("/c/benefits/STORAGE_50G/claim", null, account.token());
        assertThat(quotaExhausted.status()).isEqualTo(409);
        assertThat(quotaExhausted.bizCode()).isEqualTo("BENEFIT_QUOTA_EXHAUSTED");

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM benefit_claim WHERE period_id = ? AND status = 'SUCCESS'",
                Long.class, periodId)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM benefit_card WHERE status = 'CLAIMED' AND product_sku IN ('COFFEE_30','MOVIE_1')",
                Long.class)).isEqualTo(2);
    }

    private String suffix() {
        return String.valueOf(System.nanoTime()).substring(6, 14);
    }
}
