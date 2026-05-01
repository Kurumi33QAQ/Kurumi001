package com.zsj.modules.ums.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.modules.ums.model.UmsMemberNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 买家通知 WebSocket 发送器。
 *
 * 职责：
 * 1. 判断买家是否在线
 * 2. 把通知对象转换成前端可识别的 JSON 消息
 * 3. 通过 WebSocketSession 推送给在线买家
 *
 * 注意：推送失败不能影响订单、秒杀、通知落库等主流程。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MemberNotificationWebSocketSender {

    private final MemberWebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public void sendNotification(String memberUsername, UmsMemberNotification notification) {
        WebSocketSession session = sessionManager.get(memberUsername);
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(buildPayload(notification));

            // WebSocketSession 并发发送时可能有线程安全风险，基础版先对单个 session 加锁保护。
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket通知推送失败，memberUsername={}, notificationId={}",
                    memberUsername,
                    notification == null ? null : notification.getId(),
                    e);
        }
    }


    public void sendUnreadCount(String memberUsername, Long unreadCount) {
        WebSocketSession session = sessionManager.get(memberUsername);
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messageType", "UNREAD_COUNT_CHANGED");
            payload.put("unreadCount", unreadCount);

            String json = objectMapper.writeValueAsString(payload);

            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket未读数推送失败，memberUsername={}, unreadCount={}",
                    memberUsername,
                    unreadCount,
                    e);
        }
    }



    private Map<String, Object> buildPayload(UmsMemberNotification notification) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageType", "MEMBER_NOTIFICATION");
        payload.put("id", notification.getId());
        payload.put("title", notification.getTitle());
        payload.put("content", notification.getContent());
        payload.put("type", notification.getType());
        payload.put("businessId", notification.getBusinessId());
        payload.put("readStatus", notification.getReadStatus());
        payload.put("createTime", notification.getCreateTime());
        return payload;
    }
}
