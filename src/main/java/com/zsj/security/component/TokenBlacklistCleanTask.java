package com.zsj.security.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 黑名单清理定时任务
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TokenBlacklistCleanTask {

    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 每5分钟清理一次过期黑名单
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanExpiredBlacklist() {
        int removed = tokenBlacklistService.cleanExpired();
        if (removed > 0) {
            log.info("清理过期 token 黑名单完成，清理数量={}", removed);
        }
    }
}
