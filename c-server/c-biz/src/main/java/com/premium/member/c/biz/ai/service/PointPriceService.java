package com.premium.member.c.biz.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 能力积分单价，按类型配置。
 */
@Component
public class PointPriceService {

    private final int photoPrice;
    private final int dressupPrice;
    private final int videoPrice;

    public PointPriceService(@Value("${app.points.photo-price:100}") int photoPrice,
            @Value("${app.points.dressup-price:100}") int dressupPrice,
            @Value("${app.points.video-price:500}") int videoPrice) {
        this.photoPrice = photoPrice;
        this.dressupPrice = dressupPrice;
        this.videoPrice = videoPrice;
    }

    public int price(String type) {
        return switch (type) {
            case "VIDEO" -> videoPrice;
            case "DRESSUP" -> dressupPrice;
            default -> photoPrice;
        };
    }

    public int photoPrice() {
        return photoPrice;
    }

    public int dressupPrice() {
        return dressupPrice;
    }

    public int videoPrice() {
        return videoPrice;
    }
}
