package com.zsj.modules.ums.websocket;

import com.zsj.security.component.TokenBlacklistService;
import com.zsj.security.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 买家 WebSocket 握手拦截器。
 *
 * WebSocket 建立连接前会先进行一次 HTTP 握手。
 * 我们在握手阶段解析 token，确认当前连接属于合法买家。
 */
@RequiredArgsConstructor
@Component
public class MemberWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String ATTR_MEMBER_USERNAME = "memberUsername";

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (tokenBlacklistService.contains(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String userType = jwtTokenUtil.getUserTypeFromToken(token);
        if (!JwtTokenUtil.USER_TYPE_MEMBER.equals(userType)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        String memberUsername = jwtTokenUtil.getUserNameFromToken(token);
        if (!StringUtils.hasText(memberUsername) || !jwtTokenUtil.validateToken(token, memberUsername)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(ATTR_MEMBER_USERNAME, memberUsername);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 当前阶段不需要额外处理。
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (!StringUtils.hasText(token)) {
            return null;
        }

        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        return token;
    }
}
