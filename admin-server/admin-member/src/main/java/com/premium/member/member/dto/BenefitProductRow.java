package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 商品列表行，额外带未使用卡密库存。
 */
@Data
public class BenefitProductRow {

    private Long id;
    private String skuCode;
    private String name;
    private String description;
    private String rules;
    private Integer status;
    private Integer sort;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long stock;
}
