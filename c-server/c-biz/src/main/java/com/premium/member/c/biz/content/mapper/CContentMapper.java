package com.premium.member.c.biz.content.mapper;

import java.util.List;

import com.premium.member.c.biz.content.dto.CContentResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CContentMapper {

    @Select("""
            <script>
            SELECT DISTINCT ON (key, locale)
                   key, locale, content_json, version_no, publish_at
            FROM content_item
            WHERE status = 'PUBLISHED'
              AND publish_at &lt;= now()
              AND locale = #{locale}
            <if test="keys != null and keys.size() > 0">
              AND key IN
              <foreach collection="keys" item="item" open="(" separator="," close=")">
                #{item}
              </foreach>
            </if>
            ORDER BY key, locale, version_no DESC
            </script>
            """)
    List<CContentResponse> selectPublished(@Param("keys") List<String> keys,
            @Param("locale") String locale);
}
