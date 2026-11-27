package com.premium.member.c.biz.content.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * C 端只读取已发布内容。
 */
@Data
public class CContentResponse {

    private String key;
    private String locale;
    private String contentJson;
    private Integer versionNo;
    private OffsetDateTime publishAt;
}
