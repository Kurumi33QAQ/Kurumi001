package com.zsj.modules.sms.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体
 */
@Data
@TableName("sms_seckill_activity")
public class SmsSeckillActivity {

    private Long id;

    @TableField("product_id")
    private Long productId;

    private String name;

    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    @TableField("seckill_stock")
    private Integer seckillStock;

    @TableField("sold_count")
    private Integer soldCount;

    @TableField("per_limit")
    private Integer perLimit;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    private Integer status;

    @TableField("delete_status")
    private Integer deleteStatus;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
