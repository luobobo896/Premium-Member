package com.premium.member.c.biz.subscription.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.premium.member.c.biz.subscription.entity.PointAccount;
import com.premium.member.c.biz.subscription.entity.PointLedger;
import com.premium.member.c.biz.subscription.entity.Subscription;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubscriptionMapper {

    @Select("SELECT * FROM subscription WHERE member_id = #{memberId}")
    Subscription selectByMember(@Param("memberId") long memberId);

    @Select("SELECT * FROM subscription WHERE member_id = #{memberId} FOR UPDATE")
    Subscription lockByMember(@Param("memberId") long memberId);

    @Select("SELECT name FROM plan_version WHERE id = #{planId}")
    String selectPlanName(@Param("planId") long planId);

    @Select("SELECT monthly_points FROM plan_version WHERE id = #{planId}")
    Integer selectPlanMonthlyPoints(@Param("planId") long planId);

    @Select("SELECT price_cents FROM plan_version WHERE id = #{planId}")
    Long selectPlanPriceCents(@Param("planId") long planId);

    @Insert("""
            INSERT INTO subscription
                (member_id, plan_id, status, monthly_points, price_cents, billing_day,
                 next_charge_at, last_charge_at, grace_until, failure_reason,
                 sign_contract_no, version, created_at, updated_at)
            VALUES
                (#{memberId}, #{planId}, 'ACTIVE', #{monthlyPoints}, #{priceCents}, 1,
                 #{nextChargeAt}, #{lastChargeAt}, NULL, '', #{signContractNo}, 0, now(), now())
            ON CONFLICT (member_id) DO NOTHING
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Subscription subscription);

    @Insert("""
            INSERT INTO point_account (member_id, balance_points, updated_at)
            VALUES (#{memberId}, 0, now())
            ON CONFLICT (member_id) DO NOTHING
            """)
    int insertAccountIfAbsent(@Param("memberId") long memberId);

    @Select("SELECT * FROM point_account WHERE member_id = #{memberId} FOR UPDATE")
    PointAccount lockAccount(@Param("memberId") long memberId);

    @Select("SELECT * FROM point_account WHERE member_id = #{memberId}")
    PointAccount selectAccount(@Param("memberId") long memberId);

    @Update("""
            UPDATE point_account
            SET balance_points = balance_points + #{delta}, updated_at = now()
            WHERE member_id = #{memberId}
            """)
    int addBalance(@Param("memberId") long memberId, @Param("delta") int delta);

    @Select("SELECT * FROM point_ledger WHERE idempotency_key = #{key}")
    PointLedger selectLedgerByKey(@Param("key") String key);

    @Insert("""
            INSERT INTO point_ledger
                (member_id, delta, balance_after, type, ref_type, ref_id, period,
                 idempotency_key, operator, created_at)
            VALUES
                (#{memberId}, #{delta}, #{balanceAfter}, #{type}, #{refType}, #{refId}, #{period},
                 #{idempotencyKey}, #{operator}, now())
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    int insertLedger(@Param("memberId") long memberId, @Param("delta") int delta,
            @Param("balanceAfter") int balanceAfter, @Param("type") String type,
            @Param("refType") String refType, @Param("refId") String refId,
            @Param("period") String period, @Param("idempotencyKey") String idempotencyKey,
            @Param("operator") String operator);

    @Select("""
            <script>
            SELECT * FROM point_ledger
            WHERE member_id = #{memberId}
            <if test="month != null and month != ''">
              AND period = #{month}
            </if>
            ORDER BY created_at DESC, id DESC
            LIMIT 200
            </script>
            """)
    List<PointLedger> selectLedger(@Param("memberId") long memberId,
            @Param("month") String month);

    @Select("""
            SELECT COALESCE(SUM(delta), 0) FROM point_ledger
            WHERE member_id = #{memberId} AND period = #{period} AND type = #{type}
            """)
    int sumByPeriodAndType(@Param("memberId") long memberId, @Param("period") String period,
            @Param("type") String type);

    @Update("""
            UPDATE subscription
            SET status = #{status}, next_charge_at = #{nextChargeAt},
                last_charge_at = #{lastChargeAt}, grace_until = #{graceUntil},
                failure_reason = #{failureReason}, version = version + 1, updated_at = now()
            WHERE member_id = #{memberId}
            """)
    int updateBilling(@Param("memberId") long memberId, @Param("status") String status,
            @Param("nextChargeAt") OffsetDateTime nextChargeAt,
            @Param("lastChargeAt") OffsetDateTime lastChargeAt,
            @Param("graceUntil") OffsetDateTime graceUntil,
            @Param("failureReason") String failureReason);
}
