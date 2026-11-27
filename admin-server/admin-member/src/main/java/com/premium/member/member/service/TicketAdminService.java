package com.premium.member.member.service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.security.AuthenticatedUser;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.member.dto.TicketDetailResponse;
import com.premium.member.member.dto.TicketQuery;
import com.premium.member.member.dto.TicketRow;
import com.premium.member.member.dto.TicketUpdateRequest;
import com.premium.member.member.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * B 端工单受理、回复与会话。
 */
@Service
public class TicketAdminService {

    private static final Set<String> STATUSES =
            Set.of("OPEN", "PROCESSING", "RESOLVED", "CLOSED");
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH");

    private final TicketMapper ticketMapper;

    public TicketAdminService(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public PageResult<TicketRow> list(TicketQuery query) {
        if (query.getFrom() != null && query.getTo() != null
                && query.getFrom().isAfter(query.getTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        Page<TicketRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<TicketRow> result = ticketMapper.selectPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public TicketDetailResponse detail(String ticketNo) {
        TicketRow ticket = requireTicket(ticketNo);
        return new TicketDetailResponse(ticket,
                ticketMapper.selectReplies(ticket.getId()));
    }

    @Transactional
    public TicketDetailResponse accept(String ticketNo) {
        TicketRow ticket = requireTicket(ticketNo);
        if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.TICKET_STATE_INVALID);
        }
        ticketMapper.accept(ticketNo, SecurityUtils.currentUser().userId());
        return detail(ticketNo);
    }

    @Transactional
    public TicketDetailResponse reply(String ticketNo, String content) {
        TicketRow ticket = requireTicket(ticketNo);
        String text = content == null ? "" : content.trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "回复内容不能为空");
        }
        AuthenticatedUser user = SecurityUtils.currentUser();
        ticketMapper.insertReply(ticket.getId(), "B", user.userId(),
                StringUtils.hasText(user.realName()) ? user.realName() : user.username(), text);
        ticketMapper.touchAfterReply(ticketNo);
        return detail(ticketNo);
    }

    @Transactional
    public TicketDetailResponse update(String ticketNo, TicketUpdateRequest request) {
        TicketRow ticket = requireTicket(ticketNo);
        String status = StringUtils.hasText(request.status())
                ? request.status().trim().toUpperCase(Locale.ROOT) : ticket.getStatus();
        String priority = StringUtils.hasText(request.priority())
                ? request.priority().trim().toUpperCase(Locale.ROOT) : ticket.getPriority();
        if (!STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status 取值不合法");
        }
        if (!PRIORITIES.contains(priority)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "priority 取值不合法");
        }
        String remark = request.remark() == null
                ? ticket.getRemark() : request.remark().trim();
        Long assignee = request.assigneeAdminId() == null
                ? ticket.getAssigneeAdminId() : request.assigneeAdminId();
        OffsetDateTime resolvedAt = null;
        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            resolvedAt = ticket.getResolvedAt() == null
                    ? OffsetDateTime.now() : ticket.getResolvedAt();
        }
        ticketMapper.updateTicket(ticketNo, status, priority, remark, assignee, resolvedAt);
        return detail(ticketNo);
    }

    private TicketRow requireTicket(String ticketNo) {
        TicketRow ticket = ticketMapper.selectByNo(ticketNo);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }
}
