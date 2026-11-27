package com.premium.member.c.biz.benefit.dto;

import lombok.Data;

/**
 * 商品可用卡密库存。
 */
@Data
public class BenefitStockRow {

    private String skuCode;
    private Long stock;
}
