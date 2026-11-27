package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 领取单分页查询条件。
 */
public class BenefitClaimQuery extends PageQuery {

    private String status;
    private String productSku;
    private Long memberId;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
