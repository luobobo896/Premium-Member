package com.premium.member.member.controller;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.TicketDetailResponse;
import com.premium.member.member.dto.TicketQuery;
import com.premium.member.member.dto.TicketReplyRequest;
import com.premium.member.member.dto.TicketRow;
import com.premium.member.member.dto.TicketUpdateRequest;
import com.premium.member.member.service.TicketAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端工单。
 */
@RestController
@RequestMapping("/system/tickets")
public class SystemTicketController {

    private final TicketAdminService ticketService;

    public SystemTicketController(TicketAdminService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ticket:list')")
    public R<PageResult<TicketRow>> list(@Valid TicketQuery query) {
        return R.ok(ticketService.list(query));
    }

    @GetMapping("/{ticketNo}")
    @PreAuthorize("hasAuthority('ticket:list')")
    public R<TicketDetailResponse> detail(@PathVariable String ticketNo) {
        return R.ok(ticketService.detail(ticketNo));
    }

    @PostMapping("/{ticketNo}/accept")
    @AuditLog(module = "工单", action = "受理")
    @PreAuthorize("hasAuthority('ticket:handle')")
    public R<TicketDetailResponse> accept(@PathVariable String ticketNo) {
        return R.ok(ticketService.accept(ticketNo));
    }

    @PostMapping("/{ticketNo}/replies")
    @AuditLog(module = "工单", action = "回复")
    @PreAuthorize("hasAuthority('ticket:handle')")
    public R<TicketDetailResponse> reply(@PathVariable String ticketNo,
            @Valid @RequestBody TicketReplyRequest request) {
        return R.ok(ticketService.reply(ticketNo, request.content()));
    }

    @PutMapping("/{ticketNo}")
    @AuditLog(module = "工单", action = "改状态/备注")
    @PreAuthorize("hasAuthority('ticket:handle')")
    public R<TicketDetailResponse> update(@PathVariable String ticketNo,
            @Valid @RequestBody TicketUpdateRequest request) {
        return R.ok(ticketService.update(ticketNo, request));
    }
}
