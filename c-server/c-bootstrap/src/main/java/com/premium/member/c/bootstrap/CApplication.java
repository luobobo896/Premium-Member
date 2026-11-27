package com.premium.member.c.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.premium.member.c",
        exclude = UserDetailsServiceAutoConfiguration.class)
public class CApplication {

    public static void main(String[] args) {
        SpringApplication.run(CApplication.class, args);
    }
}
