package com.premium.member.c.biz.benefit.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.premium.member.c.biz.benefit.dto.BenefitClaimView;
import com.premium.member.c.biz.benefit.dto.BenefitStockRow;
import com.premium.member.c.biz.benefit.entity.BenefitCard;
import com.premium.member.c.biz.benefit.entity.BenefitClaim;
import com.premium.member.c.biz.benefit.entity.BenefitProduct;
import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BenefitMapper {

    @Select("""
            SELECT * FROM pm_member_period
            WHERE member_id = #{memberId}
              AND status = 'ACTIVE'
              AND start_at <= now()
              AND end_at > now()
            ORDER BY end_at DESC
            LIMIT 1
            FOR UPDATE
            """)
    PmMemberPeriod lockActivePeriod(@Param("memberId") long memberId);

    @Select("""
            SELECT * FROM pm_member_period
            WHERE member_id = #{memberId}
              AND status = 'ACTIVE'
              AND start_at <= now()
              AND end_at > now()
            ORDER BY end_at DESC
            LIMIT 1
            """)
    PmMemberPeriod selectActivePeriod(@Param("memberId") long memberId);

    @Select("""
            SELECT * FROM benefit_product
            WHERE status = 1
            ORDER BY sort, id
            """)
    List<BenefitProduct> selectActiveProducts();

    @Select("SELECT * FROM benefit_product WHERE sku_code = #{skuCode} AND status = 1 LIMIT 1")
    BenefitProduct selectActiveProduct(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM benefit_product WHERE sku_code = #{skuCode} LIMIT 1")
    BenefitProduct selectProductBySku(@Param("skuCode") String skuCode);

    @Select("""
            SELECT product_sku AS sku_code, count(*) AS stock
            FROM benefit_card
            WHERE status = 'UNUSED'
              AND (expire_at IS NULL OR expire_at > now())
            GROUP BY product_sku
            """)
    List<BenefitStockRow> selectAvailableStocks();

    @Select("SELECT * FROM benefit_claim WHERE period_id = #{periodId} AND product_sku = #{skuCode}")
    BenefitClaim selectClaim(@Param("periodId") long periodId, @Param("skuCode") String skuCode);

    @Select("""
            SELECT count(*) FROM benefit_claim
            WHERE period_id = #{periodId}
              AND status IN ('PROCESSING', 'SUCCESS', 'UNKNOWN')
            """)
    int countOccupied(@Param("periodId") long periodId);

    @Select("""
            SELECT c.id, c.product_sku, p.name AS product_name, c.status, ca.code_mask AS card_mask,
                   p.rules AS redemption, c.claimed_at, c.remark, c.created_at
            FROM benefit_claim c
            LEFT JOIN benefit_product p ON p.sku_code = c.product_sku
            LEFT JOIN benefit_card ca ON ca.id = c.card_id
            WHERE c.period_id = #{periodId}
            ORDER BY c.created_at DESC, c.id DESC
            """)
    List<BenefitClaimView> selectClaimsByPeriod(@Param("periodId") long periodId);

    @Select("""
            SELECT c.id, c.product_sku, p.name AS product_name, c.status, ca.code_mask AS card_mask,
                   p.rules AS redemption, c.claimed_at, c.remark, c.created_at
            FROM benefit_claim c
            LEFT JOIN benefit_product p ON p.sku_code = c.product_sku
            LEFT JOIN benefit_card ca ON ca.id = c.card_id
            WHERE c.member_id = #{memberId}
            ORDER BY c.created_at DESC, c.id DESC
            """)
    List<BenefitClaimView> selectHistory(@Param("memberId") long memberId);

    @Select("""
            SELECT * FROM benefit_card
            WHERE product_sku = #{skuCode}
              AND status = 'UNUSED'
              AND (expire_at IS NULL OR expire_at > now())
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    BenefitCard lockAvailableCard(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM benefit_card WHERE id = #{id}")
    BenefitCard selectCardById(@Param("id") long id);

    @Insert("""
            INSERT INTO benefit_claim
                (member_id, period_id, product_sku, status, card_id, remark, created_at, updated_at)
            VALUES
                (#{memberId}, #{periodId}, #{productSku}, #{status}, #{cardId}, #{remark}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertClaim(BenefitClaim claim);

    @Update("""
            UPDATE benefit_claim
            SET status = 'SUCCESS', card_id = #{cardId}, claimed_at = #{claimedAt},
                remark = '', updated_at = now()
            WHERE id = #{id}
            """)
    int updateClaimSuccess(@Param("id") long id, @Param("cardId") long cardId,
            @Param("claimedAt") OffsetDateTime claimedAt);

    @Update("""
            UPDATE benefit_card
            SET status = 'CLAIMED', claimed_at = #{claimedAt}, claimed_by = #{memberId}, claim_id = #{claimId}
            WHERE id = #{id} AND status = 'UNUSED'
            """)
    int markCardClaimed(@Param("id") long id, @Param("claimedAt") OffsetDateTime claimedAt,
            @Param("memberId") long memberId, @Param("claimId") long claimId);
}
