package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 会员分页查询条件。
 */
public class MemberQuery extends PageQuery {

    /** 模糊匹配会员编号、登录名、昵称、手机号。 */
    private String keyword;

    /** 等级过滤。 */
    private String level;

    /** 状态过滤：1 启用，0 停用。 */
    private Integer status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
