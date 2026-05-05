package com.zsj.modules.sms.component;

import com.zsj.modules.sms.config.SeckillRabbitMqConfig;
import com.zsj.modules.sms.dto.SeckillOrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息消费者。
 *
 * 当前阶段只验证 MQ 消费链路是否打通：
 * RabbitMQ 队列中有消息时，消费者能收到并打印日志。
 *
 * 下一步再在这里接入真正的异步创建订单逻辑。
 */
@Slf4j
@Component
public class SeckillOrderMessageConsumer {

    @RabbitListener(queues = SeckillRabbitMqConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrderMessage(SeckillOrderMessage message) {
        log.info("收到秒杀下单消息，recordId={}, activityId={}, productId={}, memberUsername={}, quantity={}, seckillPrice={}",
                message.getRecordId(),
                message.getActivityId(),
                message.getProductId(),
                message.getMemberUsername(),
                message.getQuantity(),
                message.getSeckillPrice());
    }
}
