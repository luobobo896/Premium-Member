package com.premium.member.c.biz.member.controller;

import com.premium.member.c.biz.member.dto.MemberMeResponse;
import com.premium.member.c.biz.member.service.CMemberService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/c/members")
public class CMemberController {

    private final CMemberService memberService;

    public CMemberController(CMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public R<MemberMeResponse> me() {
        return R.ok(memberService.me(SecurityUtils.currentUser().phone()));
    }
}
