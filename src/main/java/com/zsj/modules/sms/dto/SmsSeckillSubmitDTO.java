package com.zsj.modules.sms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀提交请求参数
 */
@Data
public class SmsSeckillSubmitDTO {

    /**
     * 秒杀活动ID
     */
    @NotNull(message = "秒杀活动ID不能为空")
    private Long activityId;

    /**
     * 秒杀数量，基础版先限制为1
     */
    @NotNull(message = "秒杀数量不能为空")
    @Min(value = 1, message = "秒杀数量必须大于等于1")
    private Integer quantity = 1;
}
