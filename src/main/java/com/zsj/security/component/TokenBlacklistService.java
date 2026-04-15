package com.zsj.security.component;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单服务（内存版）：
 * 1. logout 时把 token 放进黑名单
 * 2. 过滤器鉴权时检查 token 是否已被拉黑
 */
@Component
public class TokenBlacklistService {

    /**
     * key: token
     * value: 过期时间戳（毫秒）
     */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 加入黑名单
     *
     * @param token token字符串
     * @param expireAtMillis token自然过期时间（毫秒）
     */
    public void add(String token, long expireAtMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        blacklist.put(token, expireAtMillis);
    }

    /**
     * 是否在黑名单
     */
    public boolean contains(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }

        // 黑名单记录已过期则顺手清理
        if (System.currentTimeMillis() > expireAt) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 清理已过期黑名单记录（可被定时任务调用）
     */
    public int cleanExpired() {
        int removed = 0;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = blacklist.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < now) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * 仅调试用：返回黑名单数量
     */
    public int size() {
        return blacklist.size();
    }
}
