package com.zsj.modules.pms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 商品批量上下架请求参数
 */
@Data
public class PmsProductBatchPublishDTO {

    /**
     * 商品ID列表
     */
    @NotEmpty(message = "商品ID列表不能为空")
    private List<Long> ids;

    /**
     * 上架状态（0下架，1上架）
     */
    @NotNull(message = "上架状态不能为空")
    private Integer publishStatus;
}
