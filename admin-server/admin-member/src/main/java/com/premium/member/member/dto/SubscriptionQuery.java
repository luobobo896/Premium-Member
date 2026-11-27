package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionQuery extends PageQuery {

    private String status;

    private String memberNo;
}
