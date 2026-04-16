package com.zsj.security.component;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务（Redis版）：
 * 1. logout 时把 token 写入 Redis，并设置 TTL
 * 2. 过滤器鉴权时检查 token 是否在黑名单
 */
@RequiredArgsConstructor
@Component
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "security:blacklist:token:";

    private final StringRedisTemplate stringRedisTemplate;

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
        long ttlMillis = expireAtMillis - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, "1", ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 是否在黑名单
     */
    public boolean contains(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 调试用：查看黑名单数量
     */
    public int size() {
        Set<String> keys = stringRedisTemplate.keys(BLACKLIST_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }

    /**
     * Redis 方案依赖 TTL 自动过期，这里保留兼容接口
     */
    public int cleanExpired() {
        return 0;
    }
}
