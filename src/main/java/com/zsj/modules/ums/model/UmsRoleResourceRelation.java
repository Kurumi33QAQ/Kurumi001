package com.zsj.modules.ums.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-资源关系
 */
@Data
@TableName("ums_role_resource_relation")
public class UmsRoleResourceRelation {

    private Long id;
    private Long roleId;
    private Long resourceId;
}
