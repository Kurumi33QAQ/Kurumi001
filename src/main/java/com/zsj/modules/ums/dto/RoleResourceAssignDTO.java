package com.zsj.modules.ums.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色资源分配请求参数
 */
@Data
public class RoleResourceAssignDTO {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 资源ID列表（覆盖式分配）
     */
    @NotEmpty(message = "资源ID列表不能为空")
    private List<Long> resourceIds;
}
