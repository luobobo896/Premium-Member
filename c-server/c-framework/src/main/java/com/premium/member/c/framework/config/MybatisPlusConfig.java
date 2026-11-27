package com.premium.member.c.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * C 端 Mapper 扫描。
 */
@Configuration
@MapperScan({
        "com.premium.member.c.user.mapper",
        "com.premium.member.c.biz.member.mapper",
        "com.premium.member.c.biz.benefit.mapper",
        "com.premium.member.c.biz.order.mapper",
        "com.premium.member.c.biz.ai.mapper"
})
public class MybatisPlusConfig {
}
