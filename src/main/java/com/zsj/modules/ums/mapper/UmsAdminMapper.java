package com.zsj.modules.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsj.modules.ums.model.UmsAdmin;
import com.zsj.modules.ums.model.UmsRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 后台用户 Mapper
 */
public interface UmsAdminMapper extends BaseMapper<UmsAdmin> {

    /**
     * 根据用户名查询权限标识列表（如：admin:read）
     */
    java.util.List<String> getResourceNameListByUsername(String username);


    java.util.List<com.zsj.modules.ums.dto.OptionDTO> listRoleOptions();

    java.util.List<com.zsj.modules.ums.dto.OptionDTO> listResourceOptions();

    List<UmsRole> selectRolesByIds(@Param("roleIds") List<Long> roleIds);

}
