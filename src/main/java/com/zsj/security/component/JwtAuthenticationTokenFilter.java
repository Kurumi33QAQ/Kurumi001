package com.zsj.security.component;

import com.zsj.modules.ums.service.UmsAdminService;
import com.zsj.security.config.JwtProperties;
import com.zsj.security.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.zsj.modules.ums.model.UmsAdmin;
import com.zsj.modules.ums.model.UmsMember;
import com.zsj.modules.ums.service.UmsMemberService;
import java.util.List;


import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：
 * 从请求头读取 token，校验通过后把用户信息放入 SecurityContext。
 */
@RequiredArgsConstructor
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final JwtTokenUtil jwtTokenUtil;
    private final UmsAdminService umsAdminService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UmsMemberService umsMemberService;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(jwtProperties.getTokenHeader());

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(jwtProperties.getTokenHead() + " ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring((jwtProperties.getTokenHead() + " ").length());

        // 黑名单 token 直接忽略认证
        if (tokenBlacklistService.contains(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtTokenUtil.getUserNameFromToken(token);
        String userType = jwtTokenUtil.getUserTypeFromToken(token);
        String requestUri = request.getRequestURI();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(userType)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 后台接口：只接受 ADMIN token
        if (requestUri.startsWith("/demo")) {
            if (!JwtTokenUtil.USER_TYPE_ADMIN.equals(userType)) {
                filterChain.doFilter(request, response);
                return;
            }

            UmsAdmin admin = umsAdminService.getByUsername(username);
            if (admin == null || (admin.getStatus() != null && admin.getStatus() == 0)) {
                filterChain.doFilter(request, response);
                return;
            }

            List<String> authorityList = umsAdminService.getAuthorityList(username);
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

        // 买家接口：只接受 MEMBER token
        if (requestUri.startsWith("/member")) {
            if (!JwtTokenUtil.USER_TYPE_MEMBER.equals(userType)) {
                filterChain.doFilter(request, response);
                return;
            }

            UmsMember member = umsMemberService.getByUsername(username);
            if (member == null || (member.getStatus() != null && member.getStatus() == 0)) {
                filterChain.doFilter(request, response);
                return;
            }

            User userDetails = new User(username, "", List.of());

            if (jwtTokenUtil.validateToken(token, username)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
