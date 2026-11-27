package com.premium.member.c.biz.ticket.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.premium.member.c.biz.ticket.dto.CTicketCreateRequest;
import com.premium.member.c.biz.ticket.dto.CTicketView;
import com.premium.member.c.biz.ticket.mapper.CTicketMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import com.premium.member.c.framework.security.CUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * C 端工单提交与查询；只能访问自己 user_id 下的工单。
 */
@Service
public class CTicketService {

    private static final Set<String> CATEGORIES =
            Set.of("ORDER", "MEMBER", "BENEFIT", "AI", "OTHER");
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH");

    private final CTicketMapper ticketMapper;

    public CTicketService(CTicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public CTicketView create(CUserPrincipal user, CTicketCreateRequest request) {
        String category = StringUtils.hasText(request.category())
                ? request.category().trim().toUpperCase(Locale.ROOT) : "OTHER";
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "category 取值不合法");
        }
        String priority = StringUtils.hasText(request.priority())
                ? request.priority().trim().toUpperCase(Locale.ROOT) : "NORMAL";
        if (!PRIORITIES.contains(priority)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "priority 取值不合法");
        }
        String ticketNo = generateNo();
        ticketMapper.insert(ticketNo, ticketMapper.selectMemberIdByPhone(user.phone()),
                user.userId(), value(request.orderNo()), category, request.title().trim(),
                request.content().trim(), priority);
        return detail(user, ticketNo);
    }

    public List<CTicketView> list(CUserPrincipal user) {
        return ticketMapper.selectMine(user.userId());
    }

    public CTicketView detail(CUserPrincipal user, String ticketNo) {
        CTicketView ticket = ticketMapper.selectMineByNo(ticketNo, user.userId());
        if (ticket == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        ticket.setReplies(ticketMapper.selectAdminReplies(ticket.getId()));
        return ticket;
    }

    private String generateNo() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "T" + day + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }
}
