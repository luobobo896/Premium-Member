package com.premium.member.member.service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.member.crypto.CardCodeCipher;
import com.premium.member.member.dto.BenefitCardImportRequest;
import com.premium.member.member.dto.BenefitCardImportResponse;
import com.premium.member.member.dto.BenefitCardQuery;
import com.premium.member.member.dto.BenefitCardResponse;
import com.premium.member.member.dto.BenefitClaimQuery;
import com.premium.member.member.dto.BenefitClaimResponse;
import com.premium.member.member.dto.BenefitClaimRow;
import com.premium.member.member.dto.BenefitClaimReviewRequest;
import com.premium.member.member.dto.BenefitProductRequest;
import com.premium.member.member.dto.BenefitProductResponse;
import com.premium.member.member.entity.BenefitCard;
import com.premium.member.member.entity.BenefitClaim;
import com.premium.member.member.entity.BenefitProduct;
import com.premium.member.member.mapper.BenefitCardMapper;
import com.premium.member.member.mapper.BenefitClaimMapper;
import com.premium.member.member.mapper.BenefitProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 权益管理：商品、卡密与人工核实。
 */
@Service
public class BenefitAdminService {

    private final BenefitProductMapper productMapper;
    private final BenefitCardMapper cardMapper;
    private final BenefitClaimMapper claimMapper;
    private final CardCodeCipher cardCodeCipher;
    private final long lowStockThreshold;

    public BenefitAdminService(BenefitProductMapper productMapper, BenefitCardMapper cardMapper,
            BenefitClaimMapper claimMapper, CardCodeCipher cardCodeCipher,
            @Value("${app.benefit.low-stock-threshold:5}") long lowStockThreshold) {
        this.productMapper = productMapper;
        this.cardMapper = cardMapper;
        this.claimMapper = claimMapper;
        this.cardCodeCipher = cardCodeCipher;
        this.lowStockThreshold = lowStockThreshold;
    }

    public List<BenefitProductResponse> products() {
        return productMapper.selectAllWithStock().stream()
                .map(row -> BenefitProductResponse.from(row, lowStockThreshold))
                .toList();
    }

    @Transactional
    public BenefitProductResponse createProduct(BenefitProductRequest request) {
        String sku = normalizeSku(request.skuCode());
        if (productMapper.existsBySku(sku)) {
            throw new BusinessException(ErrorCode.BENEFIT_SKU_EXISTS);
        }
        OffsetDateTime now = OffsetDateTime.now();
        BenefitProduct product = new BenefitProduct();
        product.setSkuCode(sku);
        apply(product, request);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productMapper.insert(product);
        return products().stream()
                .filter(item -> item.id().equals(product.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND));
    }

    @Transactional
    public BenefitProductResponse updateProduct(long id, BenefitProductRequest request) {
        BenefitProduct product = requireProduct(id);
        if (!product.getSkuCode().equals(normalizeSku(request.skuCode()))) {
            throw new BusinessException(ErrorCode.BENEFIT_SKU_IMMUTABLE);
        }
        apply(product, request);
        product.setUpdatedAt(OffsetDateTime.now());
        productMapper.updateById(product);
        return products().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND));
    }

    /**
     * 未被卡密或领取单引用时物理删除，否则下架保留历史。
     */
    @Transactional
    public void deleteProduct(long id) {
        BenefitProduct product = requireProduct(id);
        if (cardMapper.countBySku(product.getSkuCode()) > 0
                || claimMapper.countBySku(product.getSkuCode()) > 0) {
            product.setStatus(0);
            product.setUpdatedAt(OffsetDateTime.now());
            productMapper.updateById(product);
            return;
        }
        productMapper.deleteById(id);
    }

    @Transactional
    public BenefitCardImportResponse importCards(BenefitCardImportRequest request) {
        String sku = normalizeSku(request.productSku());
        if (productMapper.selectBySku(sku) == null) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND);
        }
        String batchNo = request.batchNo().trim();
        Set<String> seen = new HashSet<>();
        int imported = 0;
        int duplicated = 0;
        for (String rawCode : request.codes()) {
            String code = rawCode.trim();
            if (code.isEmpty()) {
                continue;
            }
            if (!seen.add(code)) {
                duplicated++;
                continue;
            }
            int changed = cardMapper.insertIgnore(batchNo, sku, cardCodeCipher.hash(code),
                    cardCodeCipher.encrypt(code), cardCodeCipher.mask(code), request.expireAt());
            if (changed == 1) {
                imported++;
            } else {
                duplicated++;
            }
        }
        return new BenefitCardImportResponse(imported, duplicated);
    }

    public PageResult<BenefitCardResponse> cards(BenefitCardQuery query) {
        Page<BenefitCard> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<BenefitCard> result = cardMapper.selectPageByQuery(page, query);
        return PageResult.of(result.getRecords().stream().map(BenefitCardResponse::from).toList(),
                result.getTotal());
    }

    public PageResult<BenefitClaimResponse> claims(BenefitClaimQuery query) {
        Page<BenefitClaimRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<BenefitClaimRow> result = claimMapper.selectPageByQuery(page, query);
        return PageResult.of(result.getRecords().stream().map(BenefitClaimResponse::from).toList(),
                result.getTotal());
    }

    @Transactional
    public void reviewClaim(long id, BenefitClaimReviewRequest request) {
        BenefitClaim claim = claimMapper.selectForUpdate(id);
        if (claim == null) {
            throw new BusinessException(ErrorCode.BENEFIT_CLAIM_NOT_FOUND);
        }
        String result = request.result();
        OffsetDateTime now = OffsetDateTime.now();
        if ("FAILED".equals(result)) {
            claim.setStatus("FAILED");
            if (claim.getCardId() != null) {
                cardMapper.release(claim.getCardId(), claim.getId());
            }
            claim.setRemark(request.reason());
        } else {
            ensureCardAttached(claim);
            claim.setStatus("SUCCESS");
            claim.setClaimedAt(claim.getClaimedAt() == null ? now : claim.getClaimedAt());
            claim.setRemark("COMPENSATED".equals(result) ? "人工补偿：" + request.reason() : request.reason());
        }
        claim.setUpdatedAt(now);
        claimMapper.updateById(claim);
    }

    private void allocate(BenefitClaim claim) {
        BenefitCard card = cardMapper.lockAvailable(claim.getProductSku());
        if (card == null) {
            throw new BusinessException(ErrorCode.BENEFIT_CARD_OUT_OF_STOCK);
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (cardMapper.markClaimed(card.getId(), now, claim.getMemberId(), claim.getId()) != 1) {
            throw new BusinessException(ErrorCode.BENEFIT_CARD_OUT_OF_STOCK);
        }
        claim.setCardId(card.getId());
        claim.setClaimedAt(now);
    }

    private void ensureCardAttached(BenefitClaim claim) {
        if (claim.getCardId() != null) {
            BenefitCard card = cardMapper.selectById(claim.getCardId());
            if (card != null && "CLAIMED".equals(card.getStatus())
                    && claim.getId().equals(card.getClaimId())) {
                return;
            }
            claim.setCardId(null);
        }
        allocate(claim);
    }

    private BenefitProduct requireProduct(long id) {
        BenefitProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void apply(BenefitProduct product, BenefitProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(valueOrEmpty(request.description()));
        product.setRules(valueOrEmpty(request.rules()));
        product.setStatus(request.status() == null ? 1 : request.status());
        product.setSort(request.sort() == null ? 0 : request.sort());
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String valueOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
