package com.zsj.modules.oms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 订单分页查询参数
 */
@Data
public class OmsOrderQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize = 10;

    /**
     * 订单编号（可选，模糊）
     */
    private String orderSn;

    /**
     * 下单用户名（可选，模糊）
     */
    private String memberUsername;

    /**
     * 订单状态（可选）
     */
    private Integer status;
}
