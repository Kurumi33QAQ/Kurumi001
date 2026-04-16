package com.zsj.modules.ums.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户角色分配请求参数
 */
@Data
public class AdminRoleAssignDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long adminId;

    /**
     * 角色ID列表（覆盖式分配）
     */
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
