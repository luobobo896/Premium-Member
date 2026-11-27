package com.premium.member.c.biz.ticket.controller;

import java.util.List;

import com.premium.member.c.biz.ticket.dto.CTicketCreateRequest;
import com.premium.member.c.biz.ticket.dto.CTicketView;
import com.premium.member.c.biz.ticket.service.CTicketService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端客服工单。
 */
@RestController
@RequestMapping("/c/tickets")
public class CTicketController {

    private final CTicketService ticketService;

    public CTicketController(CTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public R<CTicketView> create(@Valid @RequestBody CTicketCreateRequest request) {
        return R.ok(ticketService.create(SecurityUtils.currentUser(), request));
    }

    @GetMapping
    public R<List<CTicketView>> list() {
        return R.ok(ticketService.list(SecurityUtils.currentUser()));
    }

    @GetMapping("/{ticketNo}")
    public R<CTicketView> detail(@PathVariable String ticketNo) {
        return R.ok(ticketService.detail(SecurityUtils.currentUser(), ticketNo));
    }
}
