package com.premium.member.member.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.ChargeQuery;
import com.premium.member.member.dto.ChargeRow;
import com.premium.member.member.dto.SubscriptionQuery;
import com.premium.member.member.dto.SubscriptionRow;
import com.premium.member.member.entity.ChargeRecord;
import com.premium.member.member.entity.Subscription;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubscriptionMapper {

    @Select("""
            <script>
            SELECT s.id, s.member_id, m.member_no, m.nickname AS member_name, m.phone,
                   p.name AS plan_name, s.status, s.monthly_points, s.price_cents,
                   COALESCE(pa.balance_points, 0) AS balance_points,
                   s.next_charge_at, s.last_charge_at, s.grace_until, s.failure_reason,
                   s.version, lc.status AS last_charge_status,
                   lc.fail_reason AS last_charge_reason, lc.created_at AS last_charge_time
            FROM subscription s
            JOIN pm_member m ON m.id = s.member_id
            LEFT JOIN plan_version p ON p.id = s.plan_id
            LEFT JOIN point_account pa ON pa.member_id = s.member_id
            LEFT JOIN LATERAL (
                SELECT status, fail_reason, created_at
                FROM charge_record c
                WHERE c.subscription_id = s.id
                ORDER BY c.created_at DESC, c.id DESC
                LIMIT 1
            ) lc ON true
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">
              AND s.status = #{query.status}
            </if>
            <if test="query.memberNo != null and query.memberNo != ''">
              AND m.member_no LIKE CONCAT('%', #{query.memberNo}, '%')
            </if>
            ORDER BY s.next_charge_at, s.id
            </script>
            """)
    IPage<SubscriptionRow> selectSubscriptionPage(Page<SubscriptionRow> page,
            @Param("query") SubscriptionQuery query);

    @Select("""
            <script>
            SELECT c.id, c.charge_no, c.subscription_id, c.member_id, m.member_no,
                   m.nickname AS member_name, m.phone, c.period, c.amount_cents, c.status,
                   c.provider, c.channel_txn_no, c.fail_reason, c.attempt_no,
                   c.created_at, c.finished_at
            FROM charge_record c
            JOIN pm_member m ON m.id = c.member_id
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">
              AND c.status = #{query.status}
            </if>
            <if test="query.memberNo != null and query.memberNo != ''">
              AND m.member_no LIKE CONCAT('%', #{query.memberNo}, '%')
            </if>
            <if test="query.period != null and query.period != ''">
              AND c.period = #{query.period}
            </if>
            <if test="query.subscriptionId != null">
              AND c.subscription_id = #{query.subscriptionId}
            </if>
            ORDER BY c.created_at DESC, c.id DESC
            </script>
            """)
    IPage<ChargeRow> selectChargePage(Page<ChargeRow> page,
            @Param("query") ChargeQuery query);

    @Select("SELECT * FROM subscription WHERE id = #{id}")
    Subscription selectById(@Param("id") long id);

    @Select("SELECT * FROM subscription WHERE id = #{id} FOR UPDATE")
    Subscription lockById(@Param("id") long id);

    @Select("""
            SELECT * FROM subscription
            WHERE status = 'ACTIVE' AND next_charge_at <= #{now}
            ORDER BY next_charge_at, id
            LIMIT #{limit}
            """)
    List<Subscription> selectDue(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    @Select("""
            SELECT * FROM subscription
            WHERE status = 'PAST_DUE'
            ORDER BY grace_until, id
            LIMIT #{limit}
            """)
    List<Subscription> selectPastDue(@Param("limit") int limit);

    @Select("""
            SELECT COALESCE(MAX(attempt_no), -1) FROM charge_record
            WHERE subscription_id = #{subscriptionId} AND period = #{period}
            """)
    int maxAttempt(@Param("subscriptionId") long subscriptionId,
            @Param("period") String period);

    @Insert("""
            INSERT INTO charge_record
                (charge_no, subscription_id, member_id, period, amount_cents, status,
                 provider, channel_txn_no, fail_reason, attempt_no, created_at)
            VALUES
                (#{chargeNo}, #{subscriptionId}, #{memberId}, #{period}, #{amountCents}, 'PENDING',
                 #{provider}, '', '', #{attemptNo}, now())
            ON CONFLICT (subscription_id, period, attempt_no) DO NOTHING
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCharge(ChargeRecord record);

    @Select("""
            SELECT * FROM charge_record
            WHERE subscription_id = #{subscriptionId} AND period = #{period}
              AND attempt_no = #{attemptNo}
            """)
    ChargeRecord selectCharge(@Param("subscriptionId") long subscriptionId,
            @Param("period") String period, @Param("attemptNo") int attemptNo);

    @Update("""
            UPDATE charge_record
            SET status = 'SUCCESS', channel_txn_no = #{txnNo}, finished_at = now()
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int markChargeSuccess(@Param("id") long id, @Param("txnNo") String txnNo);

    @Update("""
            UPDATE charge_record
            SET status = 'FAILED', fail_reason = #{reason}, finished_at = now()
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int markChargeFailed(@Param("id") long id, @Param("reason") String reason);

    @Update("""
            UPDATE subscription
            SET status = 'ACTIVE', next_charge_at = #{nextChargeAt}, last_charge_at = #{chargedAt},
                grace_until = NULL, failure_reason = '', version = version + 1, updated_at = now()
            WHERE id = #{id}
            """)
    int markActive(@Param("id") long id, @Param("nextChargeAt") OffsetDateTime nextChargeAt,
            @Param("chargedAt") OffsetDateTime chargedAt);

    @Update("""
            UPDATE subscription
            SET status = 'PAST_DUE', grace_until = #{graceUntil}, last_charge_at = #{chargedAt},
                failure_reason = #{reason}, version = version + 1, updated_at = now()
            WHERE id = #{id}
            """)
    int markPastDue(@Param("id") long id, @Param("graceUntil") OffsetDateTime graceUntil,
            @Param("chargedAt") OffsetDateTime chargedAt, @Param("reason") String reason);

    @Update("""
            UPDATE subscription
            SET status = 'SUSPENDED', failure_reason = #{reason},
                version = version + 1, updated_at = now()
            WHERE id = #{id} AND status = 'PAST_DUE'
            """)
    int markSuspended(@Param("id") long id, @Param("reason") String reason);

    @Insert("""
            INSERT INTO point_account (member_id, balance_points, updated_at)
            VALUES (#{memberId}, 0, now())
            ON CONFLICT (member_id) DO NOTHING
            """)
    int insertAccountIfAbsent(@Param("memberId") long memberId);

    @Select("SELECT balance_points FROM point_account WHERE member_id = #{memberId} FOR UPDATE")
    Integer lockBalance(@Param("memberId") long memberId);

    @Select("SELECT balance_points FROM point_account WHERE member_id = #{memberId}")
    Integer selectBalance(@Param("memberId") long memberId);

    @Update("""
            UPDATE point_account SET balance_points = balance_points + #{delta}, updated_at = now()
            WHERE member_id = #{memberId}
            """)
    int addBalance(@Param("memberId") long memberId, @Param("delta") int delta);

    @Select("SELECT idempotency_key FROM point_ledger WHERE idempotency_key = #{key}")
    String selectLedgerKey(@Param("key") String key);

    @Insert("""
            INSERT INTO point_ledger
                (member_id, delta, balance_after, type, ref_type, ref_id, period,
                 idempotency_key, operator, created_at)
            VALUES
                (#{memberId}, #{delta}, #{balanceAfter}, #{type}, #{refType}, #{refId}, #{period},
                 #{key}, #{operator}, now())
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    int insertLedger(@Param("memberId") long memberId, @Param("delta") int delta,
            @Param("balanceAfter") int balanceAfter, @Param("type") String type,
            @Param("refType") String refType, @Param("refId") String refId,
            @Param("period") String period, @Param("key") String key,
            @Param("operator") String operator);
}
