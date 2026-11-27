package com.premium.member.member.dto;

import java.util.List;

/**
 * 工单详情与会话。
 */
public record TicketDetailResponse(TicketRow ticket, List<TicketReplyRow> replies) {
}
