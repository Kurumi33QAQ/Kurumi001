package com.zsj.modules.ums.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 买家 WebSocket 消息处理器。
 *
 * 当前阶段只负责连接建立、断开和简单测试消息。
 * 后续通知中心会通过这里向在线买家推送通知。
 */
@RequiredArgsConstructor
@Component
public class MemberWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_MEMBER_USERNAME = "memberUsername";

    private final MemberWebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String memberUsername = getMemberUsername(session);
        if (memberUsername == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("缺少买家身份"));
            return;
        }

        sessionManager.add(memberUsername, session);

        session.sendMessage(new TextMessage("WebSocket连接成功，当前买家：" + memberUsername));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String memberUsername = getMemberUsername(session);

        // 当前阶段只做连通性测试，后续不把通知中心设计成自由聊天。
        session.sendMessage(new TextMessage("服务端收到消息：" + message.getPayload() + "，当前买家：" + memberUsername));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String memberUsername = getMemberUsername(session);
        if (memberUsername != null) {
            sessionManager.remove(memberUsername);
        }
    }

    private String getMemberUsername(WebSocketSession session) {
        Object value = session.getAttributes().get(ATTR_MEMBER_USERNAME);
        return value == null ? null : String.valueOf(value);
    }
}
