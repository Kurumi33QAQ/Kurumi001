package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 后台用户登录日志实体
 */
@Getter
@Setter
@TableName("ums_admin_login_log")
public class UmsAdminLoginLog {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（登录失败时可为空）
     */
    private Long adminId;

    /**
     * 登录名
     */
    private String username;

    /**
     * 客户端IP
     */
    private String ip;

    /**
     * 客户端标识（User-Agent）
     */
    private String userAgent;

    /**
     * 结果：0失败，1成功
     */
    private Integer status;

    /**
     * 结果说明
     */
    private String message;

    /**
     * 记录时间
     */
    private LocalDateTime createTime;
}
