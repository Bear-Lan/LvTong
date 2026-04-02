package com.lvtong.LvTongTransportDept.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Spring Security 相关配置
 */
@Configuration
public class SecurityConfig {

    /**
     * 注册 BCryptPasswordEncoder 为 Spring Bean
     *
     * 【说明】
     * BCryptPasswordEncoder 由 Spring Security 自动配置（SecurityAutoConfiguration）
     * 默认以 id=bcrypt 的 BCryptPasswordEncoder 注入。
     * 此处显式注册可以覆盖默认行为，方便后续统一管理。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
