package com.zsj.security.config;

import com.zsj.security.component.JwtAuthenticationTokenFilter;
import com.zsj.security.component.RestAuthenticationEntryPoint;
import com.zsj.security.component.RestfulAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：
 * 1. 指定哪些接口放行
 * 2. 接入 JWT 过滤器
 * 3. 使用无状态会话（适配 token）
 */
@EnableMethodSecurity
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    /**
     * JWT 认证过滤器（从请求头解析 token）
     */
    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestfulAccessDeniedHandler restfulAccessDeniedHandler;




    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + token 模式，通常关闭 csrf
                .csrf(csrf -> csrf.disable())
                // 无状态会话：不使用 HttpSession 存储登录态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 接口访问规则
                .authorizeHttpRequests(auth -> auth
                        // 登录、注册、基础演示接口先放行（便于调试）
                        .requestMatchers(
                                "/demo/login/simple",
                                "/demo/register/simple",
                                "/demo/token/check",
                                "/demo/logout",
                                "/demo/token/refresh",
                                "/member/auth/register",
                                "/member/auth/login",
                                "/member/auth/token/refresh",
                                "/member/auth/logout"
                        ).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                //告诉 Spring Security：以后 401 用我们自定义处理器，403 也用我们自定义处理器。
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restfulAccessDeniedHandler)
                )
                // 在用户名密码过滤器之前执行 JWT 过滤器
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 关闭默认表单登录
                .formLogin(form -> form.disable())
                // 关闭 http basic
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
