package com.zsj.modules.sms.model;

/**
 * 秒杀记录状态
 */
public final class SmsSeckillRecordStatus {

    private SmsSeckillRecordStatus() {
    }

    /**
     * 已获得秒杀资格，尚未创建订单
     */
    public static final int QUALIFIED = 0;

    /**
     * 已创建订单
     */
    public static final int ORDER_CREATED = 1;

    /**
     * 秒杀失败
     */
    public static final int FAILED = 2;

    /**
     * 已取消或已关闭，库存已补偿
     */
    public static final int CLOSED = 3;

    /**
     * 异步下单处理中：Redis 已预扣成功，等待 MQ 消费者创建订单
     */
    public static final int PROCESSING = 4;

    /**
     * 消费者已抢占该记录，正在创建订单
     */
    public static final int CREATING = 5;
}
