package com.zsj.modules.sms.component;

import com.zsj.modules.sms.config.SeckillRabbitMqConfig;
import com.zsj.modules.sms.dto.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息生产者。
 *
 * 职责：
 * 把秒杀下单请求发送到 RabbitMQ，
 * 后续由消费者异步创建订单。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SeckillOrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendSeckillOrderMessage(SeckillOrderMessage message) {
        rabbitTemplate.convertAndSend(
                SeckillRabbitMqConfig.SECKILL_ORDER_EXCHANGE,
                SeckillRabbitMqConfig.SECKILL_ORDER_ROUTING_KEY,
                message
        );

        log.info("秒杀下单消息已发送，recordId={}, activityId={}, memberUsername={}",
                message.getRecordId(),
                message.getActivityId(),
                message.getMemberUsername());
    }
}
