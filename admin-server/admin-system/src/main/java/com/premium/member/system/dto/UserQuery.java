package com.premium.member.system.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 用户分页查询条件。
 */
public class UserQuery extends PageQuery {

    /** 登录名模糊匹配。 */
    private String username;

    /** 昵称模糊匹配。 */
    private String realName;

    /** 状态过滤：1 启用，0 停用。 */
    private Integer status;

    /** 部门过滤。 */
    private Long deptId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
