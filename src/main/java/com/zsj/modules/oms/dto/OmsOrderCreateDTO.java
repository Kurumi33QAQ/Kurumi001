package com.zsj.modules.oms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 买家创建订单请求参数（基础版：单商品下单）
 */
@Data
public class OmsOrderCreateDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于等于1")
    private Integer quantity;

    /**
     * 买家备注（可选）
     */
    private String note;
}
