package com.zsj.modules.sms.service;

/**
 * 秒杀补偿业务接口
 */
public interface SmsSeckillCompensationService {

    /**
     * 秒杀订单关闭后补偿库存和秒杀记录
     */
    void compensateClosedOrder(Long activityId,
                               String memberUsername,
                               Long orderId,
                               Integer quantity);
}
