package com.zsj.modules.ums.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志清理参数
 */
@Data
public class LoginLogCleanDTO {

    /**
     * 删除该时间点之前（含该时间点）的日志
     */
    @NotNull(message = "清理截止时间不能为空")
    private LocalDateTime beforeTime;
}
