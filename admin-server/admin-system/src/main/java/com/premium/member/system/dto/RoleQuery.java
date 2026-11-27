package com.premium.member.system.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 角色分页查询条件。
 */
public class RoleQuery extends PageQuery {

    /** 角色码模糊匹配。 */
    private String code;

    /** 角色名模糊匹配。 */
    private String name;

    /** 状态过滤：1 启用，0 停用。 */
    private Integer status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
