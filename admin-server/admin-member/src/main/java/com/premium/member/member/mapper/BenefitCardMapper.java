package com.premium.member.member.mapper;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.BenefitCardQuery;
import com.premium.member.member.entity.BenefitCard;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BenefitCardMapper extends BaseMapper<BenefitCard> {

    @Insert("""
            INSERT INTO benefit_card (batch_no, product_sku, code_hash, code_cipher, code_mask, status, expire_at)
            VALUES (#{batchNo}, #{productSku}, #{codeHash}, #{codeCipher}, #{codeMask}, 'UNUSED', #{expireAt})
            ON CONFLICT (code_hash) DO NOTHING
            """)
    int insertIgnore(@Param("batchNo") String batchNo, @Param("productSku") String productSku,
            @Param("codeHash") String codeHash, @Param("codeCipher") String codeCipher,
            @Param("codeMask") String codeMask, @Param("expireAt") OffsetDateTime expireAt);

    @Select("""
            <script>
            SELECT * FROM benefit_card
            WHERE 1 = 1
            <if test="query.productSku != null and query.productSku != ''">
              AND product_sku = #{query.productSku}
            </if>
            <if test="query.batchNo != null and query.batchNo != ''">
              AND batch_no = #{query.batchNo}
            </if>
            <if test="query.status != null and query.status != ''">
              AND status = #{query.status}
            </if>
            ORDER BY id DESC
            </script>
            """)
    IPage<BenefitCard> selectPageByQuery(Page<BenefitCard> page, @Param("query") BenefitCardQuery query);

    @Select("SELECT count(*) FROM benefit_card WHERE product_sku = #{skuCode}")
    long countBySku(@Param("skuCode") String skuCode);

    @Select("""
            SELECT * FROM benefit_card
            WHERE product_sku = #{skuCode}
              AND status = 'UNUSED'
              AND (expire_at IS NULL OR expire_at > now())
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    BenefitCard lockAvailable(@Param("skuCode") String skuCode);

    @Update("""
            UPDATE benefit_card
            SET status = 'CLAIMED', claimed_at = #{claimedAt}, claimed_by = #{memberId}, claim_id = #{claimId}
            WHERE id = #{id} AND status = 'UNUSED'
            """)
    int markClaimed(@Param("id") long id, @Param("claimedAt") OffsetDateTime claimedAt,
            @Param("memberId") long memberId, @Param("claimId") long claimId);

    @Update("""
            UPDATE benefit_card
            SET status = 'UNUSED', claimed_at = NULL, claimed_by = NULL, claim_id = NULL
            WHERE id = #{id} AND status = 'CLAIMED' AND claim_id = #{claimId}
            """)
    int release(@Param("id") long id, @Param("claimId") long claimId);
}
