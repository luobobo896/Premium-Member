package com.premium.member.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * vben 分页参数：page 从 1 开始，pageSize 最大 200。
 */
public class PageQuery {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(200)
    private int pageSize = 20;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
