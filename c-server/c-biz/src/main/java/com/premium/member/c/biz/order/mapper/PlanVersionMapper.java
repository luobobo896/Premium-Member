package com.premium.member.c.biz.order.mapper;

import java.util.List;

import com.premium.member.c.biz.order.entity.PlanVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlanVersionMapper {

    @Select("""
            SELECT * FROM plan_version
            WHERE status = 1
            ORDER BY price_cents, id
            """)
    List<PlanVersion> selectOnSale();

    @Select("SELECT * FROM plan_version WHERE id = #{id} AND status = 1")
    PlanVersion selectOnSaleById(@Param("id") long id);

    @Select("SELECT * FROM plan_version WHERE id = #{id}")
    PlanVersion selectById(@Param("id") long id);
}
