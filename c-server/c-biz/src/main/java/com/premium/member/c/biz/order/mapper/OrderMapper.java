package com.premium.member.c.biz.order.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import com.premium.member.c.biz.order.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper {

    @Insert("""
            INSERT INTO orders (order_no, member_id, user_id, plan_version_id, amount_cents,
                                status, channel, source, purpose, remark, created_at, updated_at)
            VALUES (#{orderNo}, #{memberId}, #{userId}, #{planVersionId}, #{amountCents},
                    #{status}, #{channel}, #{source}, #{purpose}, #{remark}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} FOR UPDATE")
    Order lockByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND user_id = #{userId}")
    Order selectByUserAndNo(@Param("orderNo") String orderNo, @Param("userId") long userId);

    @Select("""
            SELECT * FROM orders WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Order> selectListByUser(@Param("userId") long userId);

    @Select("""
            SELECT * FROM orders
            WHERE user_id = #{userId} AND purpose = #{purpose}
              AND status IN ('CREATED', 'REFUNDING')
            LIMIT 1
            """)
    Order selectUnfinished(@Param("userId") long userId, @Param("purpose") String purpose);

    @Update("""
            UPDATE orders SET status = 'PAID', paid_at = #{paidAt}, updated_at = now()
            WHERE id = #{id} AND status = 'CREATED'
            """)
    int markPaid(@Param("id") long id, @Param("paidAt") OffsetDateTime paidAt);

    @Insert("""
            INSERT INTO payment_transactions
                (txn_no, order_id, channel, amount_cents, status, raw_payload, paid_at)
            VALUES
                (#{txnNo}, #{orderId}, #{channel}, #{amountCents}, 'SUCCESS', #{rawPayload}, #{paidAt})
            ON CONFLICT (txn_no) DO NOTHING
            """)
    int insertSuccessTransaction(@Param("txnNo") String txnNo, @Param("orderId") long orderId,
            @Param("channel") String channel, @Param("amountCents") long amountCents,
            @Param("rawPayload") String rawPayload, @Param("paidAt") OffsetDateTime paidAt);

    @Select("SELECT id FROM pm_member WHERE id = #{memberId} FOR UPDATE")
    Long lockMember(@Param("memberId") long memberId);

    @Update("""
            UPDATE pm_member_period SET status = 'EXPIRED', updated_at = now()
            WHERE member_id = #{memberId} AND status = 'ACTIVE' AND end_at <= now()
            """)
    int expireEnded(@Param("memberId") long memberId);

    @Update("""
            UPDATE pm_member_period p SET status = 'ACTIVE', updated_at = now()
            WHERE p.member_id = #{memberId} AND p.status = 'PENDING' AND p.start_at <= now()
              AND NOT EXISTS (
                  SELECT 1 FROM pm_member_period a
                  WHERE a.member_id = #{memberId} AND a.status = 'ACTIVE' AND a.id <> p.id
              )
            """)
    int activateDuePending(@Param("memberId") long memberId);

    @Select("""
            SELECT * FROM pm_member_period
            WHERE member_id = #{memberId} AND status = 'ACTIVE'
              AND start_at <= now() AND end_at > now()
            ORDER BY end_at DESC
            LIMIT 1
            """)
    PmMemberPeriod selectCurrentPeriod(@Param("memberId") long memberId);

    @Select("""
            SELECT * FROM pm_member_period
            WHERE member_id = #{memberId} AND status = 'PENDING'
            LIMIT 1
            """)
    PmMemberPeriod selectPendingPeriod(@Param("memberId") long memberId);

    @Select("SELECT * FROM pm_member_period WHERE order_id = #{orderId}")
    PmMemberPeriod selectPeriodByOrder(@Param("orderId") long orderId);

    @Insert("""
            INSERT INTO pm_member_period
                (member_id, period_no, start_at, end_at, status, order_id, created_at, updated_at)
            VALUES
                (#{memberId}, #{periodNo}, #{startAt}, #{endAt}, #{status}, #{orderId}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPeriod(PmMemberPeriod period);

    @Select("SELECT status FROM orders WHERE id = #{orderId}")
    String selectOrderStatus(@Param("orderId") long orderId);
}
