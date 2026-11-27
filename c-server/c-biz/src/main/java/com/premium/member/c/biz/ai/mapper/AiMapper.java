package com.premium.member.c.biz.ai.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.premium.member.c.biz.ai.dto.AiTaskRow;
import com.premium.member.c.biz.ai.dto.QuotaLimit;
import com.premium.member.c.biz.ai.entity.AiTask;
import com.premium.member.c.biz.ai.entity.AiTemplate;
import com.premium.member.c.biz.ai.entity.Asset;
import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiMapper {

    @Select("""
            <script>
            SELECT * FROM ai_template
            WHERE status = 1
            <if test="type != null and type != ''">
              AND type = #{type}
            </if>
            <if test="category != null and category != ''">
              AND category = #{category}
            </if>
            ORDER BY sort, id
            </script>
            """)
    List<AiTemplate> selectTemplates(@Param("type") String type,
            @Param("category") String category);

    @Select("SELECT * FROM ai_template WHERE id = #{id} AND status = 1")
    AiTemplate selectActiveTemplate(@Param("id") long id);

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

    @Select("SELECT * FROM pm_member_period WHERE id = #{id} FOR UPDATE")
    PmMemberPeriod lockPeriod(@Param("id") long id);

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
            SELECT pv.ai_image_quota AS image_limit, pv.ai_video_quota AS video_limit
            FROM pm_member_period pd
            LEFT JOIN orders o ON o.id = pd.order_id
            LEFT JOIN plan_version pv ON pv.id = o.plan_version_id
            WHERE pd.id = #{periodId}
            """)
    QuotaLimit selectQuotaLimit(@Param("periodId") long periodId);

    @Select("""
            SELECT COALESCE(SUM(delta), 0) FROM quota_ledger
            WHERE period_id = #{periodId} AND kind = #{kind}
            """)
    int sumDelta(@Param("periodId") long periodId, @Param("kind") String kind);

    @Insert("""
            INSERT INTO quota_ledger
                (period_id, kind, delta, balance_after, reason, ref_task_id,
                 idempotency_key, operator, created_at)
            VALUES
                (#{periodId}, #{kind}, #{delta}, #{balanceAfter}, #{reason}, #{refTaskId},
                 #{idempotencyKey}, #{operator}, now())
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    int insertLedger(@Param("periodId") long periodId, @Param("kind") String kind,
            @Param("delta") int delta, @Param("balanceAfter") int balanceAfter,
            @Param("reason") String reason, @Param("refTaskId") long refTaskId,
            @Param("idempotencyKey") String idempotencyKey, @Param("operator") String operator);

    @Insert("""
            INSERT INTO ai_task
                (task_no, member_id, period_id, template_id, type, status,
                 input_asset_id, output_asset_id, supplier_task_no, fail_reason,
                 cost_cents, idempotency_key, mock_result, created_at)
            VALUES
                (#{taskNo}, #{memberId}, #{periodId}, #{templateId}, #{type}, #{status},
                 #{inputAssetId}, #{outputAssetId}, #{supplierTaskNo}, #{failReason},
                 #{costCents}, #{idempotencyKey}, #{mockResult}, now())
            ON CONFLICT (idempotency_key) DO NOTHING
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTask(AiTask task);

    @Select("SELECT * FROM ai_task WHERE idempotency_key = #{key}")
    AiTask selectTaskByIdem(@Param("key") String key);

    @Select("SELECT * FROM ai_task WHERE task_no = #{taskNo} AND member_id = #{memberId}")
    AiTask selectTaskByNo(@Param("taskNo") String taskNo, @Param("memberId") long memberId);

    @Select("SELECT * FROM ai_task WHERE id = #{id}")
    AiTask selectTaskById(@Param("id") long id);

    @Select("SELECT * FROM ai_task WHERE task_no = #{taskNo}")
    AiTask selectAnyTaskByNo(@Param("taskNo") String taskNo);

    @Select("""
            SELECT t.id, t.task_no, t.period_id, t.template_id, tp.name AS template_name,
                   t.type, t.status, t.input_asset_id, ia.storage_key AS input_storage_key,
                   t.output_asset_id, oa.storage_key AS output_storage_key, oa.mime AS output_mime,
                   t.fail_reason, t.cost_cents, t.created_at, t.finished_at
            FROM ai_task t
            LEFT JOIN ai_template tp ON tp.id = t.template_id
            LEFT JOIN asset ia ON ia.id = t.input_asset_id
            LEFT JOIN asset oa ON oa.id = t.output_asset_id AND oa.status = 'ACTIVE'
            WHERE t.task_no = #{taskNo} AND t.member_id = #{memberId}
            """)
    AiTaskRow selectTaskRow(@Param("taskNo") String taskNo, @Param("memberId") long memberId);

    @Select("""
            <script>
            SELECT t.id, t.task_no, t.period_id, t.template_id, tp.name AS template_name,
                   t.type, t.status, t.input_asset_id, ia.storage_key AS input_storage_key,
                   t.output_asset_id, oa.storage_key AS output_storage_key, oa.mime AS output_mime,
                   t.fail_reason, t.cost_cents, t.created_at, t.finished_at
            FROM ai_task t
            LEFT JOIN ai_template tp ON tp.id = t.template_id
            LEFT JOIN asset ia ON ia.id = t.input_asset_id
            LEFT JOIN asset oa ON oa.id = t.output_asset_id AND oa.status = 'ACTIVE'
            WHERE t.member_id = #{memberId}
            <if test="status != null and status != ''">
              AND t.status = #{status}
            </if>
            ORDER BY t.created_at DESC, t.id DESC
            LIMIT 100
            </script>
            """)
    List<AiTaskRow> selectTaskRows(@Param("memberId") long memberId,
            @Param("status") String status);

    @Update("""
            UPDATE ai_task SET status = 'RUNNING'
            WHERE task_no = #{taskNo} AND status = 'QUEUED'
            """)
    int markRunning(@Param("taskNo") String taskNo);

    @Update("""
            UPDATE ai_task
            SET status = 'SUCCESS', output_asset_id = #{outputAssetId},
                supplier_task_no = #{supplierTaskNo}, fail_reason = '', finished_at = #{finishedAt}
            WHERE task_no = #{taskNo} AND status = 'RUNNING'
            """)
    int markSuccess(@Param("taskNo") String taskNo, @Param("outputAssetId") long outputAssetId,
            @Param("supplierTaskNo") String supplierTaskNo,
            @Param("finishedAt") OffsetDateTime finishedAt);

    @Update("""
            UPDATE ai_task
            SET status = #{status}, fail_reason = #{reason}, finished_at = #{finishedAt}
            WHERE task_no = #{taskNo} AND status = 'RUNNING'
            """)
    int markTerminal(@Param("taskNo") String taskNo, @Param("status") String status,
            @Param("reason") String reason, @Param("finishedAt") OffsetDateTime finishedAt);

    @Insert("""
            INSERT INTO asset
                (owner_member_id, purpose, mime, size, storage_key, status, expire_at, created_at)
            VALUES
                (#{ownerMemberId}, #{purpose}, #{mime}, #{size}, #{storageKey}, #{status},
                 #{expireAt}, now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAsset(Asset asset);

    @Select("SELECT * FROM asset WHERE id = #{id}")
    Asset selectAsset(@Param("id") long id);

    @Select("""
            SELECT * FROM asset
            WHERE storage_key = #{storageKey} AND status = 'ACTIVE'
              AND (expire_at IS NULL OR expire_at > now())
            """)
    Asset selectActiveAssetByKey(@Param("storageKey") String storageKey);

    @Update("UPDATE asset SET status = 'DELETED' WHERE id = #{id} AND status = 'ACTIVE'")
    int markAssetDeleted(@Param("id") long id);
}
