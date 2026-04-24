package com.zsj.modules.oms.component;

import com.zsj.modules.oms.service.OmsOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时自动取消任务（基础版）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OmsOrderTimeoutCancelTask {

    /**
     * 基础版超时时间：30分钟
     */
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;

    private final OmsOrderService omsOrderService;

    /**
     * 每分钟扫描一次超时未支付订单并关闭
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void autoCancelTimeoutOrders() {
        int count = omsOrderService.autoCancelTimeoutOrders(DEFAULT_TIMEOUT_MINUTES);
        if (count > 0) {
            log.info("自动取消超时未支付订单完成，数量={}", count);
        }
    }
}
