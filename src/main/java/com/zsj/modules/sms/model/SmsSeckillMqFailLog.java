package com.zsj.modules.sms.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀 MQ 失败日志实体。
 *
 * 用于把死信队列中的失败消息落库，方便后台查询、人工排查和后续补偿。
 */
@Data
@TableName("sms_seckill_mq_fail_log")
public class SmsSeckillMqFailLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long activityId;

    private Long productId;

    private String memberUsername;

    private Integer quantity;

    private BigDecimal seckillPrice;

    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private String failReason;

    private Integer failCount;

    /**
     * 处理状态：0待处理，1已重投，2已忽略，3处理成功，4处理失败。
     */
    private Integer handleStatus;

    private Integer requeueCount;

    private LocalDateTime lastRequeueTime;

    private String handleRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
