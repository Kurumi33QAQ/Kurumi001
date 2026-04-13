package com.zsj.modules.ums.service;

import com.zsj.modules.ums.dto.AdminInfoDTO;
import com.zsj.modules.ums.model.UmsAdmin;

import java.util.List;

/**
 * 后台用户业务接口
 */
public interface UmsAdminService {

    /**
     * 查询所有后台用户
     */
    List<UmsAdmin> listAll();

    /**
     * 按用户名查询
     */
    UmsAdmin getByUsername(String username);

    /**
     * 登录校验（先不生成 token，先打通用户名/密码校验）
     * @return 校验通过返回提示信息，失败返回 null（下一步会优化成业务错误码）
     */
    AdminInfoDTO login(String username, String password);

    /**
     * 注册（先不生成 token，先打通用户名/密码注册）
     * @return 注册成功返回用户 id，失败返回 null（下一步会优化成业务错误码）
     */
    Long register(String username, String password);

    /**
     * 根据用户名获取脱敏用户信息
     */
    AdminInfoDTO getAdminInfo(String username);


    /**
     * 根据用户名获取权限列表
     */
    java.util.List<String> getAuthorityList(String username);

}
