package com.premium.member.c.biz.benefit.controller;

import java.util.List;

import com.premium.member.c.biz.benefit.dto.BenefitCenterResponse;
import com.premium.member.c.biz.benefit.dto.BenefitClaimHistoryItem;
import com.premium.member.c.biz.benefit.dto.BenefitClaimResult;
import com.premium.member.c.biz.benefit.service.CBenefitService;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端权益中心。
 */
@RestController
@RequestMapping("/c/benefits")
public class CBenefitController {

    private final CBenefitService benefitService;

    public CBenefitController(CBenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping
    public R<BenefitCenterResponse> center() {
        return R.ok(benefitService.center(SecurityUtils.currentUser().phone()));
    }

    @PostMapping("/{sku}/claim")
    public R<BenefitClaimResult> claim(@PathVariable String sku) {
        return R.ok(benefitService.claim(SecurityUtils.currentUser().phone(), sku));
    }

    @GetMapping("/claims")
    public R<List<BenefitClaimHistoryItem>> claims() {
        return R.ok(benefitService.history(SecurityUtils.currentUser().phone()));
    }
}
