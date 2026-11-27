package com.premium.member.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.BenefitClaimQuery;
import com.premium.member.member.dto.BenefitClaimRow;
import com.premium.member.member.entity.BenefitClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BenefitClaimMapper extends BaseMapper<BenefitClaim> {

    @Select("""
            <script>
            SELECT c.id, c.member_id, m.member_no, m.nickname AS member_name,
                   c.period_id, p.period_no, c.product_sku, pr.name AS product_name,
                   c.status, c.card_id, ca.code_mask AS card_mask,
                   c.claimed_at, c.remark, c.created_at
            FROM benefit_claim c
            LEFT JOIN pm_member m ON m.id = c.member_id
            LEFT JOIN pm_member_period p ON p.id = c.period_id
            LEFT JOIN benefit_product pr ON pr.sku_code = c.product_sku
            LEFT JOIN benefit_card ca ON ca.id = c.card_id
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">
              AND c.status = #{query.status}
            </if>
            <if test="query.productSku != null and query.productSku != ''">
              AND c.product_sku = #{query.productSku}
            </if>
            <if test="query.memberId != null">
              AND c.member_id = #{query.memberId}
            </if>
            ORDER BY c.created_at DESC, c.id DESC
            </script>
            """)
    IPage<BenefitClaimRow> selectPageByQuery(Page<BenefitClaimRow> page,
            @Param("query") BenefitClaimQuery query);

    @Select("SELECT count(*) FROM benefit_claim WHERE product_sku = #{skuCode}")
    long countBySku(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM benefit_claim WHERE id = #{id} FOR UPDATE")
    BenefitClaim selectForUpdate(@Param("id") long id);
}
