package com.zsj.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 先关闭 csrf，便于本地用 apifox 直接调 POST
                .csrf(csrf -> csrf.disable())
                // 允许所有请求（后续做 JWT 时再收紧）
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 关闭默认登录页
                .formLogin(form -> form.disable())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
