package com.premium.member.c.biz.benefit.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.premium.member.c.biz.benefit.crypto.CardCodeCipher;
import com.premium.member.c.biz.benefit.dto.BenefitCenterResponse;
import com.premium.member.c.biz.benefit.dto.BenefitClaimHistoryItem;
import com.premium.member.c.biz.benefit.dto.BenefitClaimResult;
import com.premium.member.c.biz.benefit.dto.BenefitClaimView;
import com.premium.member.c.biz.benefit.dto.BenefitStockRow;
import com.premium.member.c.biz.benefit.entity.BenefitCard;
import com.premium.member.c.biz.benefit.entity.BenefitClaim;
import com.premium.member.c.biz.benefit.entity.BenefitProduct;
import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import com.premium.member.c.biz.benefit.mapper.BenefitMapper;
import com.premium.member.c.biz.member.entity.PmMember;
import com.premium.member.c.biz.member.mapper.PmMemberMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C 端权益中心与领取。
 */
@Service
public class CBenefitService {

    private static final int QUOTA_PER_PERIOD = 2;
    private static final Set<String> OCCUPIED = Set.of("PROCESSING", "SUCCESS", "UNKNOWN");

    private final BenefitMapper benefitMapper;
    private final PmMemberMapper memberMapper;
    private final CardCodeCipher cardCodeCipher;

    public CBenefitService(BenefitMapper benefitMapper, PmMemberMapper memberMapper,
            CardCodeCipher cardCodeCipher) {
        this.benefitMapper = benefitMapper;
        this.memberMapper = memberMapper;
        this.cardCodeCipher = cardCodeCipher;
    }

    @Transactional
    public BenefitCenterResponse center(String phone) {
        PmMember member = requireActiveMember(phone);
        benefitMapper.expireEnded(member.getId());
        benefitMapper.activateDuePending(member.getId());
        PmMemberPeriod period = benefitMapper.selectActivePeriod(member.getId());
        if (period == null) {
            return new BenefitCenterResponse(null, QUOTA_PER_PERIOD, 0, QUOTA_PER_PERIOD, List.of());
        }
        Map<String, BenefitClaimView> claims = benefitMapper.selectClaimsByPeriod(period.getId()).stream()
                .collect(Collectors.toMap(BenefitClaimView::getProductSku, Function.identity(),
                        (left, right) -> left));
        Map<String, Long> stocks = benefitMapper.selectAvailableStocks().stream()
                .collect(Collectors.toMap(BenefitStockRow::getSkuCode,
                        row -> row.getStock() == null ? 0L : row.getStock()));
        int used = (int) claims.values().stream().filter(item -> OCCUPIED.contains(item.getStatus())).count();
        List<BenefitCenterResponse.Product> products = benefitMapper.selectActiveProducts().stream()
                .map(product -> {
                    BenefitClaimView claim = claims.get(product.getSkuCode());
                    boolean claimed = claim != null && OCCUPIED.contains(claim.getStatus());
                    return new BenefitCenterResponse.Product(
                            product.getSkuCode(),
                            product.getName(),
                            product.getDescription(),
                            product.getRules(),
                            claimed,
                            claim == null ? null : claim.getStatus(),
                            claim == null ? null : claim.getCardMask(),
                            claim == null ? null : claim.getClaimedAt(),
                            stocks.getOrDefault(product.getSkuCode(), 0L) > 0);
                })
                .toList();
        BenefitCenterResponse.Period info = new BenefitCenterResponse.Period(
                period.getId(), period.getPeriodNo(), period.getStartAt(), period.getEndAt());
        return new BenefitCenterResponse(info, QUOTA_PER_PERIOD, used,
                Math.max(0, QUOTA_PER_PERIOD - used), products);
    }

    /**
     * 事务内占名额并锁卡密；重复点击返回同一领取单，确切失败的单允许复用重试。
     */
    @Transactional
    public BenefitClaimResult claim(String phone, String skuCode) {
        PmMember member = requireActiveMember(phone);
        benefitMapper.expireEnded(member.getId());
        benefitMapper.activateDuePending(member.getId());
        PmMemberPeriod period = benefitMapper.lockActivePeriod(member.getId());
        if (period == null) {
            throw new BusinessException(ErrorCode.BENEFIT_PERIOD_NOT_FOUND);
        }
        if (period.getOrderId() != null && "REFUNDING".equals(
                benefitMapper.selectOrderStatus(period.getOrderId()))) {
            throw new BusinessException(ErrorCode.ORDER_REFUNDING);
        }
        String sku = skuCode.trim().toUpperCase(Locale.ROOT);
        BenefitClaim existing = benefitMapper.selectClaim(period.getId(), sku);
        if (existing != null && OCCUPIED.contains(existing.getStatus())) {
            return existingResult(existing);
        }
        BenefitProduct product = benefitMapper.selectActiveProduct(sku);
        if (product == null) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND);
        }
        int occupied = benefitMapper.countOccupied(period.getId());
        if (occupied >= QUOTA_PER_PERIOD) {
            throw new BusinessException(ErrorCode.BENEFIT_QUOTA_EXHAUSTED);
        }
        BenefitClaim claim;
        if (existing == null) {
            claim = new BenefitClaim();
            claim.setMemberId(member.getId());
            claim.setPeriodId(period.getId());
            claim.setProductSku(sku);
            claim.setStatus("PROCESSING");
            claim.setRemark("");
            benefitMapper.insertClaim(claim);
        } else {
            claim = existing;
        }
        BenefitCard card = benefitMapper.lockAvailableCard(sku);
        if (card == null) {
            throw new BusinessException(ErrorCode.BENEFIT_CARD_OUT_OF_STOCK);
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (benefitMapper.markCardClaimed(card.getId(), now, member.getId(), claim.getId()) != 1) {
            throw new BusinessException(ErrorCode.BENEFIT_CARD_OUT_OF_STOCK);
        }
        benefitMapper.updateClaimSuccess(claim.getId(), card.getId(), now);
        return new BenefitClaimResult(
                claim.getId(),
                sku,
                product.getName(),
                "SUCCESS",
                false,
                cardCodeCipher.decrypt(card.getCodeCipher()),
                card.getCodeMask(),
                now,
                product.getRules());
    }

    @Transactional(readOnly = true)
    public List<BenefitClaimHistoryItem> history(String phone) {
        PmMember member = requireActiveMember(phone);
        return benefitMapper.selectHistory(member.getId()).stream()
                .map(BenefitClaimHistoryItem::from)
                .toList();
    }

    private BenefitClaimResult existingResult(BenefitClaim claim) {
        BenefitProduct product = benefitMapper.selectProductBySku(claim.getProductSku());
        BenefitCard card = claim.getCardId() == null ? null : benefitMapper.selectCardById(claim.getCardId());
        return new BenefitClaimResult(
                claim.getId(),
                claim.getProductSku(),
                product == null ? claim.getProductSku() : product.getName(),
                claim.getStatus(),
                true,
                null,
                card == null ? null : card.getCodeMask(),
                claim.getClaimedAt(),
                product == null ? null : product.getRules());
    }

    private PmMember requireActiveMember(String phone) {
        PmMember member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);
        }
        return member;
    }
}
