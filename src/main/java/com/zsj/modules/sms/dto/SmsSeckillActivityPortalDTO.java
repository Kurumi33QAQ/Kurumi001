package com.zsj.modules.sms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 买家端秒杀活动返回对象
 */
@Data
public class SmsSeckillActivityPortalDTO {

    private Long id;
    private Long productId;
    private String name;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer soldCount;
    private Integer perLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;

    /**
     * 动态展示状态：0未启用,1未开始,2进行中,3已结束,4已售罄
     */
    private Integer activityStatus;

    /**
     * 动态展示状态文案
     */
    private String activityStatusText;
}
