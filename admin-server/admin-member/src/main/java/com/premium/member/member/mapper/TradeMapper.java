package com.premium.member.member.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.OrderQuery;
import com.premium.member.member.dto.OrderRow;
import com.premium.member.member.dto.PaymentQuery;
import com.premium.member.member.dto.PaymentRow;
import com.premium.member.member.entity.Order;
import com.premium.member.member.entity.PaymentTransaction;
import com.premium.member.member.entity.PmMemberPeriod;
import com.premium.member.member.entity.Refund;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TradeMapper {

    @Select("""
            SELECT id FROM plan_version
            WHERE code = #{code}
            ORDER BY version_no DESC
            LIMIT 1
            FOR UPDATE
            """)
    Long lockPlanCode(@Param("code") String code);

    @Select("""
            SELECT * FROM plan_version
            ORDER BY code, version_no DESC
            """)
    List<com.premium.member.member.entity.PlanVersion> selectPlans();

    @Select("SELECT * FROM plan_version WHERE id = #{id}")
    com.premium.member.member.entity.PlanVersion selectPlan(@Param("id") long id);

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM plan_version WHERE code = #{code}")
    int maxPlanVersion(@Param("code") String code);

    @Update("UPDATE plan_version SET status = 0 WHERE code = #{code} AND status = 1")
    int disablePlanCode(@Param("code") String code);

    @Update("UPDATE plan_version SET status = 0 WHERE id = #{id}")
    int disablePlan(@Param("id") long id);

    @Insert("""
            INSERT INTO plan_version
                (code, name, price_cents, duration_days, ai_image_quota, ai_video_quota,
                 benefit_quota, status, version_no, created_at)
            VALUES
                (#{code}, #{name}, #{priceCents}, #{durationDays}, #{aiImageQuota}, #{aiVideoQuota},
                 #{benefitQuota}, #{status}, #{versionNo}, now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlan(com.premium.member.member.entity.PlanVersion plan);

    @Select("""
            <script>
            SELECT o.*, m.member_no, m.nickname AS member_name, m.phone,
                   p.code AS plan_code, p.name AS plan_name, p.version_no AS plan_version_no,
                   p.price_cents AS plan_price_cents, p.duration_days, p.ai_image_quota,
                   p.ai_video_quota, p.benefit_quota,
                   pd.id AS period_id, pd.period_no, pd.start_at AS period_start_at,
                   pd.end_at AS period_end_at, pd.status AS period_status
            FROM orders o
            LEFT JOIN pm_member m ON m.id = o.member_id
            LEFT JOIN plan_version p ON p.id = o.plan_version_id
            LEFT JOIN pm_member_period pd ON pd.order_id = o.id
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">
              AND o.status = #{query.status}
            </if>
            <if test="query.orderNo != null and query.orderNo != ''">
              AND o.order_no LIKE CONCAT('%', #{query.orderNo}, '%')
            </if>
            <if test="query.memberId != null">
              AND o.member_id = #{query.memberId}
            </if>
            ORDER BY o.created_at DESC, o.id DESC
            </script>
            """)
    IPage<OrderRow> selectOrderPage(Page<OrderRow> page, @Param("query") OrderQuery query);

    @Select("""
            SELECT o.*, m.member_no, m.nickname AS member_name, m.phone,
                   p.code AS plan_code, p.name AS plan_name, p.version_no AS plan_version_no,
                   p.price_cents AS plan_price_cents, p.duration_days, p.ai_image_quota,
                   p.ai_video_quota, p.benefit_quota,
                   pd.id AS period_id, pd.period_no, pd.start_at AS period_start_at,
                   pd.end_at AS period_end_at, pd.status AS period_status
            FROM orders o
            LEFT JOIN pm_member m ON m.id = o.member_id
            LEFT JOIN plan_version p ON p.id = o.plan_version_id
            LEFT JOIN pm_member_period pd ON pd.order_id = o.id
            WHERE o.order_no = #{orderNo}
            """)
    OrderRow selectOrderRow(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} FOR UPDATE")
    Order lockOrder(@Param("orderNo") String orderNo);

    @Update("""
            UPDATE orders SET status = #{status}, updated_at = now()
            WHERE id = #{id}
            """)
    int updateOrderStatus(@Param("id") long id, @Param("status") String status);

    @Select("""
            SELECT * FROM payment_transactions
            WHERE order_id = #{orderId}
            ORDER BY created_at DESC, id DESC
            """)
    List<PaymentTransaction> selectPaymentsByOrder(@Param("orderId") long orderId);

    @Select("""
            SELECT * FROM refunds
            WHERE order_id = #{orderId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Refund> selectRefundsByOrder(@Param("orderId") long orderId);

    @Select("""
            SELECT * FROM refunds
            WHERE order_id = #{orderId} AND status IN ('APPLIED', 'APPROVED')
            LIMIT 1
            """)
    Refund selectActiveRefund(@Param("orderId") long orderId);

    @Insert("""
            INSERT INTO refunds
                (refund_no, order_id, amount_cents, status, operator, reason, created_at, updated_at)
            VALUES
                (#{refundNo}, #{orderId}, #{amountCents}, #{status}, #{operator}, #{reason}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRefund(Refund refund);

    @Update("""
            UPDATE refunds
            SET status = #{status}, updated_at = now(),
                processed_at = CASE WHEN #{status} IN ('REJECTED', 'SUCCESS', 'FAILED') THEN now() ELSE processed_at END
            WHERE id = #{id}
            """)
    int updateRefundStatus(@Param("id") long id, @Param("status") String status);

    @Select("SELECT * FROM pm_member_period WHERE order_id = #{orderId}")
    PmMemberPeriod selectPeriodByOrder(@Param("orderId") long orderId);

    @Select("""
            SELECT count(*) FROM benefit_claim
            WHERE period_id = #{periodId} AND status IN ('PROCESSING', 'UNKNOWN')
            """)
    long countClaimInFlight(@Param("periodId") long periodId);

    @Update("""
            UPDATE pm_member_period SET status = 'TERMINATED', updated_at = now()
            WHERE order_id = #{orderId} AND status IN ('ACTIVE', 'PENDING')
            """)
    int terminatePeriod(@Param("orderId") long orderId);

    @Select("""
            <script>
            SELECT t.*, o.order_no, m.member_no, m.nickname AS member_name
            FROM payment_transactions t
            LEFT JOIN orders o ON o.id = t.order_id
            LEFT JOIN pm_member m ON m.id = o.member_id
            WHERE 1 = 1
            <if test="query.channel != null and query.channel != ''">
              AND t.channel = #{query.channel}
            </if>
            <if test="query.txnNo != null and query.txnNo != ''">
              AND t.txn_no LIKE CONCAT('%', #{query.txnNo}, '%')
            </if>
            <if test="query.orderNo != null and query.orderNo != ''">
              AND o.order_no LIKE CONCAT('%', #{query.orderNo}, '%')
            </if>
            ORDER BY t.created_at DESC, t.id DESC
            </script>
            """)
    IPage<PaymentRow> selectPaymentPage(Page<PaymentRow> page, @Param("query") PaymentQuery query);
}
