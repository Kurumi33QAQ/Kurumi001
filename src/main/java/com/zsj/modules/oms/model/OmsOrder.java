package com.zsj.modules.oms.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("oms_order")
public class OmsOrder {

    private Long id;
    private String orderSn;
    private String memberUsername;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer status;
    private String note;
    private Integer deleteStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableField("product_id")
    private Long productId;

    @TableField("product_quantity")
    private Integer productQuantity;

    @TableField("close_type")
    private Integer closeType;


}
