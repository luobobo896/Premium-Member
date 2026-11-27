package com.premium.member.c.biz.benefit.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 卡密解密。加密写入在 admin-server，双端共用 BENEFIT_CARD_SECRET。
 */
@Component
public class CardCodeCipher {

    private static final Logger log = LoggerFactory.getLogger(CardCodeCipher.class);
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final Counter decryptFailures;

    public CardCodeCipher(@Value("${app.benefit.card-secret}") String secret, MeterRegistry meterRegistry) {
        this.key = new SecretKeySpec(sha256(secret), "AES");
        this.decryptFailures = Counter.builder("benefit.card.decrypt.failures")
                .description("卡密解密失败次数")
                .register(meterRegistry);
    }

    public String decrypt(String cipherText) {
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException | ArrayIndexOutOfBoundsException ex) {
            decryptFailures.increment();
            log.warn("卡密解密失败，按卡片失效处理：{}，累计 {} 次",
                    ex.getClass().getSimpleName(), (long) decryptFailures.count());
            throw new BusinessException(ErrorCode.BENEFIT_CARD_INVALID);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
