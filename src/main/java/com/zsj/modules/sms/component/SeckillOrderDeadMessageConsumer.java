package com.zsj.modules.sms.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.modules.sms.config.SeckillRabbitMqConfig;
import com.zsj.modules.sms.dto.SeckillOrderMessage;
import com.zsj.modules.sms.mapper.SmsSeckillMqFailLogMapper;
import com.zsj.modules.sms.model.SmsSeckillMqFailLog;
import com.zsj.modules.sms.model.SmsSeckillMqFailLogHandleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 秒杀下单死信消息消费者。
 *
 * 只负责把死信消息落入失败日志表，不在这里自动重投，避免形成反复失败循环。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderDeadMessageConsumer {

    private final SmsSeckillMqFailLogMapper smsSeckillMqFailLogMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE)
    public void handleDeadMessage(SeckillOrderMessage message) {
        log.warn("收到秒杀下单死信消息，recordId={}, activityId={}, memberUsername={}",
                message.getRecordId(), message.getActivityId(), message.getMemberUsername());

        SmsSeckillMqFailLog failLog = new SmsSeckillMqFailLog();
        failLog.setRecordId(message.getRecordId());
        failLog.setActivityId(message.getActivityId());
        failLog.setProductId(message.getProductId());
        failLog.setMemberUsername(message.getMemberUsername());
        failLog.setQuantity(message.getQuantity());
        failLog.setSeckillPrice(message.getSeckillPrice());
        failLog.setQueueName(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE);
        failLog.setExchangeName(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_EXCHANGE);
        failLog.setRoutingKey(SeckillRabbitMqConfig.SECKILL_ORDER_DEAD_ROUTING_KEY);
        failLog.setMessageBody(toJson(message));
        failLog.setFailReason("秒杀下单消息多次消费失败后进入死信队列");
        failLog.setFailCount(1);
        failLog.setHandleStatus(SmsSeckillMqFailLogHandleStatus.PENDING);
        failLog.setRequeueCount(0);
        failLog.setCreateTime(LocalDateTime.now());
        failLog.setUpdateTime(LocalDateTime.now());

        smsSeckillMqFailLogMapper.insert(failLog);
    }

    private String toJson(SeckillOrderMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.warn("序列化秒杀死信消息失败，recordId={}", message.getRecordId(), e);
            return "{}";
        }
    }
}
