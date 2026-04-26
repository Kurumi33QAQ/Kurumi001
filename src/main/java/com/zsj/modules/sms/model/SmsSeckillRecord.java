package com.zsj.modules.sms.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀记录实体
 */
@Data
@TableName("sms_seckill_record")
public class SmsSeckillRecord {

    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("member_username")
    private String memberUsername;

    private Integer quantity;

    /**
     * 状态:0已获得资格,1已创建订单,2失败
     */
    private Integer status;

    @TableField("order_id")
    private Long orderId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
