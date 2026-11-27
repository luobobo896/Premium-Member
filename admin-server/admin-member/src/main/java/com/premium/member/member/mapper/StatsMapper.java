package com.premium.member.member.mapper;

import java.time.LocalDate;
import java.util.List;

import com.premium.member.member.dto.DailyStatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatsMapper {

    @Select("""
            <script>
            WITH days AS (
                SELECT generate_series(#{from}::date, #{to}::date, interval '1 day')::date AS day
            )
            SELECT to_char(d.day, 'YYYY-MM-DD') AS day,
                   (SELECT count(*) FROM orders o
                    WHERE o.created_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND o.created_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND o.source = #{source}
                    </if>) AS order_count,
                   (SELECT COALESCE(sum(o.amount_cents), 0) FROM orders o
                    WHERE o.status = 'PAID'
                      AND o.paid_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND o.paid_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND o.source = #{source}
                    </if>) AS paid_amount_cents,
                   (SELECT count(DISTINCT p.member_id) FROM pm_member_period p
                    WHERE p.status IN ('ACTIVE', 'EXPIRED')
                      AND p.start_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND p.end_at &gt; (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND p.member_id IN (SELECT o2.member_id FROM orders o2
                                          WHERE o2.source = #{source})
                    </if>) AS active_members,
                   (SELECT count(*) FROM benefit_claim c
                    WHERE c.created_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND c.created_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND c.member_id IN (SELECT o2.member_id FROM orders o2
                                          WHERE o2.source = #{source})
                    </if>) AS benefit_applied,
                   (SELECT count(*) FROM benefit_claim c
                    WHERE c.status = 'SUCCESS'
                      AND c.created_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND c.created_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND c.member_id IN (SELECT o2.member_id FROM orders o2
                                          WHERE o2.source = #{source})
                    </if>) AS benefit_success,
                   (SELECT count(*) FROM benefit_claim c
                    WHERE c.status IN ('FAILED', 'UNKNOWN')
                      AND c.created_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND c.created_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND c.member_id IN (SELECT o2.member_id FROM orders o2
                                          WHERE o2.source = #{source})
                    </if>) AS benefit_exception,
                   (SELECT count(*) FROM ai_task t
                    WHERE t.type IN ('PHOTO', 'DRESSUP')
                      AND t.created_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND t.created_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND t.member_id IN (SELECT o2.member_id FROM orders o2
                                          WHERE o2.source = #{source})
                    </if>) AS ai_image_tasks,
                   (SELECT count(*) FROM ai_task t
                    WHERE t.type = 'VIDEO'
                      AND t.created_at &gt;= (d.day::timestamp AT TIME ZONE 'Asia/Shanghai')
                      AND t.created_at &lt; ((d.day + 1)::timestamp AT TIME ZONE 'Asia/Shanghai')
                    <if test="source != null and source != ''">
                      AND t.member_id IN (SELECT o2.member_id FROM orders o2
                                          WHERE o2.source = #{source})
                    </if>) AS ai_video_tasks
            FROM days d
            ORDER BY d.day DESC
            </script>
            """)
    List<DailyStatRow> selectDaily(@Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("source") String source);
}
