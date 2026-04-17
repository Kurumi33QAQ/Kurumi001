package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 后台用户表（ums_admin）实体
 */
@Getter
@Setter
@ToString(exclude = "password")
@TableName("ums_admin")
public class UmsAdmin {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String icon;
    private String email;
    private String nickName;
    private String note;
    private LocalDateTime createTime;
    private LocalDateTime loginTime;
    private Integer status;
    /**
     * 连续登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 账号锁定截止时间（为空表示未锁定）
     */
    private LocalDateTime lockExpireTime;

}
