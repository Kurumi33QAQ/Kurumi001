package com.zsj.modules.sms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀异步下单消息。
 *
 * MQ 消息只放消费者创建订单需要的最小字段，
 * 不直接传完整活动对象，避免消息体过大和字段耦合。
 */
@Data
public class SeckillOrderMessage {

    /**
     * 秒杀记录ID。
     * 后续消费者用它更新秒杀结果。
     */
    private Long recordId;

    /**
     * 失败日志ID。
     *
     * 正常秒杀消息为空；从失败日志手动重投时会携带该字段，
     * 后续消费者处理成功后可据此回写失败日志状态。
     */
    private Long failLogId;

    private Long activityId;

    private Long productId;

    private String memberUsername;

    private Integer quantity;

    private BigDecimal seckillPrice;

    private LocalDateTime createTime;
}
