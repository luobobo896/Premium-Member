package com.premium.member.member.mapper;

import java.time.LocalDate;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.AiTaskQuery;
import com.premium.member.member.dto.AiTaskRow;
import com.premium.member.member.dto.AiUsageRow;
import com.premium.member.member.entity.AiTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiAdminMapper {

    @Select("SELECT * FROM ai_template ORDER BY sort, id")
    List<AiTemplate> selectTemplates();

    @Select("SELECT * FROM ai_template WHERE id = #{id}")
    AiTemplate selectTemplate(@Param("id") long id);

    @Insert("""
            INSERT INTO ai_template
                (code, name, type, category, cover_url, params, status, version_no, sort,
                 created_at, updated_at)
            VALUES
                (#{code}, #{name}, #{type}, #{category}, #{coverUrl}, CAST(#{params} AS jsonb),
                 #{status}, #{versionNo}, #{sort}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTemplate(AiTemplate template);

    @Update("""
            UPDATE ai_template
            SET name = #{name}, type = #{type}, category = #{category}, cover_url = #{coverUrl},
                params = CAST(#{params} AS jsonb), status = #{status}, sort = #{sort},
                version_no = version_no + 1, updated_at = now()
            WHERE id = #{id}
            """)
    int updateTemplate(@Param("id") long id, @Param("name") String name,
            @Param("type") String type, @Param("category") String category,
            @Param("coverUrl") String coverUrl, @Param("params") String params,
            @Param("status") int status, @Param("sort") int sort);

    @Delete("DELETE FROM ai_template WHERE id = #{id}")
    int deleteTemplate(@Param("id") long id);

    @Select("""
            <script>
            SELECT t.id, t.task_no, t.member_id, m.member_no, m.nickname AS member_name, m.phone,
                   t.period_id, pd.period_no, t.template_id, tp.code AS template_code,
                   tp.name AS template_name, t.type, t.status, t.supplier_task_no,
                   t.fail_reason, t.cost_cents, t.created_at, t.finished_at
            FROM ai_task t
            LEFT JOIN pm_member m ON m.id = t.member_id
            LEFT JOIN pm_member_period pd ON pd.id = t.period_id
            LEFT JOIN ai_template tp ON tp.id = t.template_id
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">
              AND t.status = #{query.status}
            </if>
            <if test="query.type != null and query.type != ''">
              AND t.type = #{query.type}
            </if>
            <if test="query.memberId != null">
              AND t.member_id = #{query.memberId}
            </if>
            <if test="query.taskNo != null and query.taskNo != ''">
              AND t.task_no LIKE CONCAT('%', #{query.taskNo}, '%')
            </if>
            ORDER BY t.created_at DESC, t.id DESC
            </script>
            """)
    IPage<AiTaskRow> selectTaskPage(Page<AiTaskRow> page, @Param("query") AiTaskQuery query);

    @Select("""
            SELECT to_char(created_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM-DD') AS day,
                   count(*) AS total,
                   count(*) FILTER (WHERE status = 'SUCCESS') AS success,
                   count(*) FILTER (WHERE status = 'FAILED') AS failed,
                   count(*) FILTER (WHERE status = 'REJECTED') AS rejected,
                   count(*) FILTER (WHERE status = 'UNKNOWN') AS unknown,
                   count(*) FILTER (WHERE type IN ('PHOTO', 'DRESSUP')) AS image_tasks,
                   count(*) FILTER (WHERE type = 'VIDEO') AS video_tasks,
                   COALESCE(sum(cost_cents), 0) AS cost_cents
            FROM ai_task
            WHERE created_at >= #{from}::date
              AND created_at < (#{to}::date + interval '1 day')
            GROUP BY 1
            ORDER BY 1 DESC
            """)
    List<AiUsageRow> selectUsage(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
