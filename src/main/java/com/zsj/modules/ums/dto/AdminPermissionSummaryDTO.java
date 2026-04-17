package com.zsj.modules.ums.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户权限汇总DTO：
 * 展示用户基本信息 + 角色名称列表 + 权限点列表
 */
@Data
public class AdminPermissionSummaryDTO {

    /**
     * 用户ID
     */
    private Long adminId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色名称列表
     */
    private List<String> roleNames;

    /**
     * 权限点列表（例如：admin:read）
     */
    private List<String> authorities;
}
