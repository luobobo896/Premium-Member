package com.premium.member.member.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.member.dto.BenefitProductRow;
import com.premium.member.member.entity.BenefitProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BenefitProductMapper extends BaseMapper<BenefitProduct> {

    @Select("SELECT EXISTS (SELECT 1 FROM benefit_product WHERE sku_code = #{skuCode})")
    boolean existsBySku(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM benefit_product WHERE sku_code = #{skuCode} LIMIT 1")
    BenefitProduct selectBySku(@Param("skuCode") String skuCode);

    @Select("""
            SELECT p.id, p.sku_code, p.name, p.description, p.rules, p.status, p.sort,
                   p.created_at, p.updated_at,
                   (SELECT count(*) FROM benefit_card c
                    WHERE c.product_sku = p.sku_code
                      AND c.status = 'UNUSED'
                      AND (c.expire_at IS NULL OR c.expire_at > now())) AS stock
            FROM benefit_product p
            ORDER BY p.sort, p.id
            """)
    List<BenefitProductRow> selectAllWithStock();
}
