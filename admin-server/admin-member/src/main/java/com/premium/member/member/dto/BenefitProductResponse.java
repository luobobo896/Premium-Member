package com.premium.member.member.dto;

import java.time.OffsetDateTime;

/**
 * 权益商品响应。
 *
 * @param stock    未使用且未过期的卡密数量
 * @param lowStock 库存低于阈值
 */
public record BenefitProductResponse(
        Long id,
        String skuCode,
        String name,
        String description,
        String rules,
        Integer status,
        Integer sort,
        Long stock,
        boolean lowStock,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static BenefitProductResponse from(BenefitProductRow row, long threshold) {
        long stock = row.getStock() == null ? 0 : row.getStock();
        return new BenefitProductResponse(
                row.getId(),
                row.getSkuCode(),
                row.getName(),
                row.getDescription(),
                row.getRules(),
                row.getStatus(),
                row.getSort(),
                stock,
                stock < threshold,
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
