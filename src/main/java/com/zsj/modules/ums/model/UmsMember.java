package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 买家用户表（ums_member）实体
 */
@Getter
@Setter
@ToString(exclude = "password")
@TableName("ums_member")
public class UmsMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickName;
    private String phone;
    private String email;
    private Integer status;
    private Integer loginFailCount;
    private LocalDateTime lockExpireTime;
    private Integer deleteStatus;
    private LocalDateTime createTime;
    private LocalDateTime loginTime;
}
