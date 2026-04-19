package com.zsj.modules.pms.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 商品批量删除请求参数
 */
@Data
public class PmsProductBatchDeleteDTO {

    /**
     * 商品ID列表
     */
    @NotEmpty(message = "商品ID列表不能为空")
    private List<Long> ids;
}
