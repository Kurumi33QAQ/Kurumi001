package com.zsj.modules.pms.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("pms_product")
public class PmsProduct {

    private Long id;
    private String name;
    private String subTitle;
    private BigDecimal price;
    private Integer stock;
    private Integer sale;
    private String pic;
    private Integer publishStatus;
    private Integer deleteStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
