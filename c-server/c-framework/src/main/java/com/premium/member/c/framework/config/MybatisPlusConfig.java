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
        "com.premium.member.c.biz.subscription.mapper",
        "com.premium.member.c.biz.ai.mapper",
        "com.premium.member.c.biz.ticket.mapper",
        "com.premium.member.c.biz.content.mapper"
})
public class MybatisPlusConfig {
}
