package com.zsj.modules.pms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 商品分页查询参数
 */
@Data
public class PmsProductQueryDTO {

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize = 10;

    /**
     * 商品名称（可选，模糊查询）
     */
    private String name;

    /**
     * 上架状态（可选：0下架，1上架）
     */
    private Integer publishStatus;
}
