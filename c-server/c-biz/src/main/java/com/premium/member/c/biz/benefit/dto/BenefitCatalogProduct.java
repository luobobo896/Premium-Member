package com.premium.member.c.biz.benefit.dto;

/**
 * 游客可见的权益目录项，不包含库存、名额与领取状态。
 *
 * @param cover 展示封面标识，前端映射到设计系统渐变封面
 */
public record BenefitCatalogProduct(
        String skuCode,
        String name,
        String description,
        String rules,
        String cover) {
}
