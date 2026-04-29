package com.zsj.modules.ums.config;

import com.zsj.modules.ums.websocket.MemberWebSocketHandler;
import com.zsj.modules.ums.websocket.MemberWebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 买家 WebSocket 配置。
 */
@RequiredArgsConstructor
@EnableWebSocket
@Configuration
public class MemberWebSocketConfig implements WebSocketConfigurer {

    private final MemberWebSocketHandler memberWebSocketHandler;
    private final MemberWebSocketHandshakeInterceptor memberWebSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(memberWebSocketHandler, "/ws/member")
                .addInterceptors(memberWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
