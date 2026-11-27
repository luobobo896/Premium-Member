package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 卡密分页查询条件。
 */
public class BenefitCardQuery extends PageQuery {

    private String productSku;
    private String batchNo;
    private String status;

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
