package com.zsj.modules.oms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单状态更新参数
 */
@Data
public class OmsOrderStatusUpdateDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    /**
     * 订单状态：0待付款,1待发货,2已发货,3已完成,4已关闭
     */
    @NotNull(message = "订单状态不能为空")
    private Integer status;

    /**
     * 后台备注（可选）
     */
    private String note;
}
