package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChargeQuery extends PageQuery {

    private String status;

    private String memberNo;

    private String period;

    private Long subscriptionId;
}
