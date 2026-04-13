package com.zsj.security.component;

import com.zsj.modules.ums.service.UmsAdminService;
import com.zsj.security.config.JwtProperties;
import com.zsj.security.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：
 * 从请求头读取 token，校验通过后把用户信息放入 SecurityContext。
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final JwtTokenUtil jwtTokenUtil;
    private final UmsAdminService umsAdminService;

    public JwtAuthenticationTokenFilter(JwtProperties jwtProperties,
                                        JwtTokenUtil jwtTokenUtil,
                                        UmsAdminService umsAdminService) {
        this.jwtProperties = jwtProperties;
        this.jwtTokenUtil = jwtTokenUtil;
        this.umsAdminService = umsAdminService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(jwtProperties.getTokenHeader());

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(jwtProperties.getTokenHead() + " ")) {
            String token = authHeader.substring((jwtProperties.getTokenHead() + " ").length());
            String username = jwtTokenUtil.getUserNameFromToken(token);

            // 当前线程上下文中还没有认证信息时，尝试设置
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 根据用户名动态加载权限
                java.util.List<String> authorityList = umsAdminService.getAuthorityList(username);

                User userDetails = new User(
                        username,
                        "",
                        authorityList.stream()
                                .map(authority -> (org.springframework.security.core.GrantedAuthority) () -> authority)
                                .toList()
                );


                if (jwtTokenUtil.validateToken(token, username)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
