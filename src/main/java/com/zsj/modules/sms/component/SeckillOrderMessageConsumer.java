package com.zsj.modules.sms.component;

import com.zsj.modules.sms.config.SeckillRabbitMqConfig;
import com.zsj.modules.sms.dto.SeckillOrderMessage;
import com.zsj.modules.sms.service.SmsSeckillActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息消费者。
 *
 * RabbitMQ 队列中有消息时，消费者负责调用业务层异步创建订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderMessageConsumer {

    private final SmsSeckillActivityService smsSeckillActivityService;

    @RabbitListener(queues = SeckillRabbitMqConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrderMessage(SeckillOrderMessage message) {
        log.info("收到秒杀下单消息，recordId={}, activityId={}, productId={}, memberUsername={}, quantity={}, seckillPrice={}",
                message.getRecordId(),
                message.getActivityId(),
                message.getProductId(),
                message.getMemberUsername(),
                message.getQuantity(),
                message.getSeckillPrice());

        smsSeckillActivityService.createOrderFromMessage(message);
    }
}
