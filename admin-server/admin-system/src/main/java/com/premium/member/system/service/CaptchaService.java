package com.premium.member.system.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.cache.CacheKeys;
import com.premium.member.system.dto.CaptchaResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 服务端图形验证码，验证码文本存 Redis，5 分钟过期。
 */
@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final CacheKeys cacheKeys;

    public CaptchaService(StringRedisTemplate redisTemplate, CacheKeys cacheKeys) {
        this.redisTemplate = redisTemplate;
        this.cacheKeys = cacheKeys;
    }

    /**
     * 生成验证码并返回 PNG data URL。
     *
     * @return 验证码 ID 与图片
     */
    public CaptchaResponse generate() {
        String captchaId = UUID.randomUUID().toString();
        String code = randomCode();
        redisTemplate.opsForValue().set(cacheKeys.captcha(captchaId), code, TTL);
        return new CaptchaResponse(captchaId, "data:image/png;base64," + render(code));
    }

    /**
     * 校验并消费验证码，校验失败抛 400。
     *
     * @param captchaId   验证码 ID
     * @param captchaCode 用户输入
     */
    public void validate(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "验证码不能为空");
        }
        String key = cacheKeys.captcha(captchaId);
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (expected == null || !expected.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "验证码错误或已过期");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            builder.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(247, 248, 250));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(new Color(20 + random.nextInt(80), 20 + random.nextInt(80),
                        20 + random.nextInt(80)));
                graphics.drawString(String.valueOf(code.charAt(i)), 18 + i * 24, 29 + random.nextInt(5));
            }
            graphics.setColor(new Color(180, 190, 200));
            for (int i = 0; i < 5; i++) {
                graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码生成失败");
        }
    }
}
