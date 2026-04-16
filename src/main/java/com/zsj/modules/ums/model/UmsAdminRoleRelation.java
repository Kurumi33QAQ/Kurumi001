package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 后台用户-角色关系
 */
@Data
@TableName("ums_admin_role_relation")
public class UmsAdminRoleRelation {

    private Long id;
    private Long adminId;
    private Long roleId;
}
