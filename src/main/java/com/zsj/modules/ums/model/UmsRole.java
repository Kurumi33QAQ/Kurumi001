package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体
 */
@Data
@TableName("ums_role")
public class UmsRole {
    private Long id;
    private String name;
}
