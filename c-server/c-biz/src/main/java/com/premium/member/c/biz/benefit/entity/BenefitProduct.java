package com.premium.member.c.biz.benefit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权益商品。
 */
@Data
@TableName("benefit_product")
public class BenefitProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuCode;

    private String name;

    private String description;

    private String rules;

    private Integer status;

    private Integer sort;
}
