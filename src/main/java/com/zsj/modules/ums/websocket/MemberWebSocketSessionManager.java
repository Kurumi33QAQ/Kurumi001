package com.zsj.modules.ums.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 买家 WebSocket 在线连接管理器。
 *
 * 基础版使用本地内存保存在线用户连接：
 * key 是 memberUsername，value 是 WebSocketSession。
 *
 * 注意：这种方式适合单体应用。
 * 如果后续多实例部署，需要升级为 Redis 维护在线状态和消息路由。
 */
@Component
public class MemberWebSocketSessionManager {

    private final Map<String, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    public void add(String memberUsername, WebSocketSession session) {
        onlineSessions.put(memberUsername, session);
    }

    public void remove(String memberUsername) {
        onlineSessions.remove(memberUsername);
    }

    public WebSocketSession get(String memberUsername) {
        return onlineSessions.get(memberUsername);
    }

    public boolean isOnline(String memberUsername) {
        WebSocketSession session = onlineSessions.get(memberUsername);
        return session != null && session.isOpen();
    }

    public int onlineCount() {
        return onlineSessions.size();
    }
}
