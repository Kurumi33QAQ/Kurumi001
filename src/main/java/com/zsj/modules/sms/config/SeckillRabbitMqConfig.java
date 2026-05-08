package com.zsj.modules.sms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀 RabbitMQ 配置。
 *
 * 定义秒杀下单正常队列、死信队列和 JSON 消息转换器。
 */
@Configuration
public class SeckillRabbitMqConfig {

    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";
    public static final String SECKILL_ORDER_DEAD_EXCHANGE = "seckill.order.dlx";
    public static final String SECKILL_ORDER_DEAD_QUEUE = "seckill.order.dlq";
    public static final String SECKILL_ORDER_DEAD_ROUTING_KEY = "seckill.order.dead";

    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .deadLetterExchange(SECKILL_ORDER_DEAD_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_ORDER_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillOrderBinding(DirectExchange seckillOrderExchange,
                                       Queue seckillOrderQueue) {
        return BindingBuilder.bind(seckillOrderQueue)
                .to(seckillOrderExchange)
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillOrderDeadExchange() {
        return new DirectExchange(SECKILL_ORDER_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderDeadQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_DEAD_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderDeadBinding(DirectExchange seckillOrderDeadExchange,
                                           Queue seckillOrderDeadQueue) {
        return BindingBuilder.bind(seckillOrderDeadQueue)
                .to(seckillOrderDeadExchange)
                .with(SECKILL_ORDER_DEAD_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
