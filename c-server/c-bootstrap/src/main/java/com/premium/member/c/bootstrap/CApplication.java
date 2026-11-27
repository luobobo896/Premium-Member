package com.premium.member.c.bootstrap;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.premium.member.c",
        exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
public class CApplication {

    public static void main(String[] args) {
        SpringApplication.run(CApplication.class, args);
    }
}
