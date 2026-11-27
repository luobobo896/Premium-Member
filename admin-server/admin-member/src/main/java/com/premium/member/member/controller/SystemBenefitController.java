package com.premium.member.member.controller;

import java.util.List;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.BenefitCardImportRequest;
import com.premium.member.member.dto.BenefitCardImportResponse;
import com.premium.member.member.dto.BenefitCardQuery;
import com.premium.member.member.dto.BenefitCardResponse;
import com.premium.member.member.dto.BenefitClaimQuery;
import com.premium.member.member.dto.BenefitClaimResponse;
import com.premium.member.member.dto.BenefitClaimReviewRequest;
import com.premium.member.member.dto.BenefitProductRequest;
import com.premium.member.member.dto.BenefitProductResponse;
import com.premium.member.member.service.BenefitAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权益管理：商品、卡密、领取单。
 */
@RestController
@RequestMapping("/system/benefit")
public class SystemBenefitController {

    private final BenefitAdminService benefitService;

    public SystemBenefitController(BenefitAdminService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('benefit:product:list')")
    public R<List<BenefitProductResponse>> products() {
        return R.ok(benefitService.products());
    }

    @PostMapping("/products")
    @AuditLog(module = "权益", action = "新增商品")
    @PreAuthorize("hasAuthority('benefit:product:create')")
    public R<BenefitProductResponse> createProduct(@Valid @RequestBody BenefitProductRequest request) {
        return R.ok(benefitService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    @AuditLog(module = "权益", action = "编辑商品")
    @PreAuthorize("hasAuthority('benefit:product:update')")
    public R<BenefitProductResponse> updateProduct(@PathVariable long id,
            @Valid @RequestBody BenefitProductRequest request) {
        return R.ok(benefitService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    @AuditLog(module = "权益", action = "删除商品")
    @PreAuthorize("hasAuthority('benefit:product:delete')")
    public R<Void> deleteProduct(@PathVariable long id) {
        benefitService.deleteProduct(id);
        return R.ok();
    }

    @GetMapping("/cards")
    @PreAuthorize("hasAuthority('benefit:card:list')")
    public R<PageResult<BenefitCardResponse>> cards(@Valid BenefitCardQuery query) {
        return R.ok(benefitService.cards(query));
    }

    @PostMapping("/cards/import")
    @AuditLog(module = "权益", action = "导入卡密")
    @PreAuthorize("hasAuthority('benefit:card:import')")
    public R<BenefitCardImportResponse> importCards(@Valid @RequestBody BenefitCardImportRequest request) {
        return R.ok(benefitService.importCards(request));
    }

    @GetMapping("/claims")
    @PreAuthorize("hasAuthority('benefit:claim:list')")
    public R<PageResult<BenefitClaimResponse>> claims(@Valid BenefitClaimQuery query) {
        return R.ok(benefitService.claims(query));
    }

    @PostMapping("/claims/{id}/review")
    @AuditLog(module = "权益", action = "人工核实")
    @PreAuthorize("hasAuthority('benefit:claim:review')")
    public R<Void> reviewClaim(@PathVariable long id,
            @Valid @RequestBody BenefitClaimReviewRequest request) {
        benefitService.reviewClaim(id, request);
        return R.ok();
    }
}
