package com.premium.member.c.biz.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 创作模板。
 */
@Data
@TableName("ai_template")
public class AiTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String type;

    private String category;

    private String coverUrl;

    private String params;

    private Integer status;

    private Integer versionNo;

    private Integer sort;
}
