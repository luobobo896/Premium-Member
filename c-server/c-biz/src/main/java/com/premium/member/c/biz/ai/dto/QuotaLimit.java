package com.premium.member.c.biz.ai.dto;

import lombok.Data;

/**
 * 周期绑定的套餐额度。
 */
@Data
public class QuotaLimit {

    private Integer imageLimit;

    private Integer videoLimit;

    public static QuotaLimit defaults() {
        QuotaLimit limit = new QuotaLimit();
        limit.setImageLimit(20);
        limit.setVideoLimit(3);
        return limit;
    }
}
