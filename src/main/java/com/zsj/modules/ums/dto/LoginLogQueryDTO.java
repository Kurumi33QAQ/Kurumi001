package com.zsj.modules.ums.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 登录日志分页查询参数
 */
@Data
public class LoginLogQueryDTO {

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页数量（限制最大100，防止一次查太多）
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer pageSize = 10;

    /**
     * 用户名（可选，模糊匹配）
     */
    private String username;

    /**
     * 登录状态（可选：0失败，1成功）
     */
    private Integer status;

    /**
     * 开始时间（可选）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间（可选）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
