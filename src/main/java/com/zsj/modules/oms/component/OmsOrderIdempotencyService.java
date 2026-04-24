package com.zsj.modules.oms.component;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 订单创建幂等服务（基础版）
 */
@Component
@RequiredArgsConstructor
public class OmsOrderIdempotencyService {

    //Redis 锁 key 前缀，表示“某个幂等请求正在处理”
    private static final String ORDER_CREATE_LOCK_PREFIX = "oms:order:create:lock:";

    //Redis 结果 key 前缀，表示“该幂等请求已成功，订单号是多少”
    private static final String ORDER_CREATE_RESULT_PREFIX = "oms:order:create:result:";

    //锁最多占用 20 秒，防止异常时死锁
    private static final long LOCK_TTL_SECONDS = 20;

    //成功结果保留 30 分钟，方便重复请求直接返回历史结果
    private static final long RESULT_TTL_MINUTES = 30;

    private final StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String memberUsername, String idempotencyKey) {
        String key = buildLockKey(memberUsername, idempotencyKey);
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    public void releaseLock(String memberUsername, String idempotencyKey) {
        stringRedisTemplate.delete(buildLockKey(memberUsername, idempotencyKey));
    }

    public Long getCreatedOrderId(String memberUsername, String idempotencyKey) {
        String value = stringRedisTemplate.opsForValue().get(buildResultKey(memberUsername, idempotencyKey));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void markSuccess(String memberUsername, String idempotencyKey, Long orderId) {
        String resultKey = buildResultKey(memberUsername, idempotencyKey);
        stringRedisTemplate.opsForValue()
                .set(resultKey, String.valueOf(orderId), RESULT_TTL_MINUTES, TimeUnit.MINUTES);
        releaseLock(memberUsername, idempotencyKey);
    }

    private String buildLockKey(String memberUsername, String idempotencyKey) {
        return ORDER_CREATE_LOCK_PREFIX + memberUsername + ":" + idempotencyKey;
    }

    private String buildResultKey(String memberUsername, String idempotencyKey) {
        return ORDER_CREATE_RESULT_PREFIX + memberUsername + ":" + idempotencyKey;
    }
}
