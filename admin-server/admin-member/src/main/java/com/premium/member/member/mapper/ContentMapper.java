package com.premium.member.member.mapper;

import java.util.List;

import com.premium.member.member.entity.ContentItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContentMapper {

    @Select("""
            <script>
            SELECT DISTINCT ON (key, locale) *
            FROM content_item
            WHERE 1 = 1
            <if test="key != null and key != ''">
              AND key = #{key}
            </if>
            <if test="locale != null and locale != ''">
              AND locale = #{locale}
            </if>
            ORDER BY key, locale, version_no DESC
            </script>
            """)
    List<ContentItem> selectLatest(@Param("key") String key, @Param("locale") String locale);

    @Select("""
            SELECT * FROM content_item
            WHERE key = #{key} AND locale = #{locale}
            ORDER BY version_no DESC
            """)
    List<ContentItem> selectVersions(@Param("key") String key, @Param("locale") String locale);

    @Select("SELECT * FROM content_item WHERE id = #{id}")
    ContentItem selectById(@Param("id") long id);

    @Select("SELECT COALESCE(max(version_no), 0) FROM content_item WHERE key = #{key} AND locale = #{locale}")
    int selectMaxVersion(@Param("key") String key, @Param("locale") String locale);

    @Select("""
            SELECT * FROM content_item
            WHERE key = #{key} AND locale = #{locale} AND status = 'DRAFT'
            ORDER BY version_no DESC LIMIT 1
            """)
    ContentItem selectLatestDraft(@Param("key") String key, @Param("locale") String locale);

    @Insert("""
            INSERT INTO content_item
                (key, locale, content_json, version_no, status, updated_by, created_at, updated_at)
            VALUES
                (#{key}, #{locale}, CAST(#{contentJson} AS jsonb), #{versionNo}, 'DRAFT',
                 #{updatedBy}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentItem item);

    @Update("""
            UPDATE content_item
            SET content_json = CAST(#{contentJson} AS jsonb), updated_by = #{updatedBy},
                updated_at = now()
            WHERE id = #{id} AND status = 'DRAFT'
            """)
    int updateDraft(@Param("id") long id, @Param("contentJson") String contentJson,
            @Param("updatedBy") String updatedBy);

    @Update("""
            UPDATE content_item
            SET status = 'PUBLISHED', publish_at = now(), updated_by = #{updatedBy},
                updated_at = now()
            WHERE id = #{id} AND status = 'DRAFT'
            """)
    int publish(@Param("id") long id, @Param("updatedBy") String updatedBy);

    @Insert("""
            INSERT INTO content_item
                (key, locale, content_json, version_no, status, publish_at, updated_by,
                 created_at, updated_at)
            SELECT key, locale, content_json, #{versionNo}, 'PUBLISHED', now(), #{updatedBy},
                   now(), now()
            FROM content_item WHERE id = #{id}
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRollback(ContentItem item);
}
