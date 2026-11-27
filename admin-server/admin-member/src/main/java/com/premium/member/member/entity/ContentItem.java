package com.premium.member.member.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 内容配置版本。行不可覆盖，发布与回滚都新增版本。
 */
@Data
@TableName("content_item")
public class ContentItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String key;

    private String locale;

    private String contentJson;

    private Integer versionNo;

    private String status;

    private OffsetDateTime publishAt;

    private String updatedBy;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
