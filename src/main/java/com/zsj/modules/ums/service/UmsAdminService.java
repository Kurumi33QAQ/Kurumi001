package com.zsj.modules.ums.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.modules.ums.dto.AdminInfoDTO;
import com.zsj.modules.ums.dto.LoginLogQueryDTO;
import com.zsj.modules.ums.model.UmsAdmin;
import com.zsj.modules.ums.model.UmsAdminLoginLog;

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
     * 登录校验（带审计信息）
     */
    AdminInfoDTO login(String username, String password, String ip, String userAgent);

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

    /**
     * 清理指定用户的权限缓存
     */
    void evictAuthorityCache(String username);

    /**
     * 获取当前权限缓存中的用户数量
     */
    int getAuthorityCacheSize();


    /**
     * 登录日志分页查询
     */
    IPage<UmsAdminLoginLog> pageLoginLogs(LoginLogQueryDTO queryDTO);


    /**
     * 按条件查询登录日志（用于导出，不分页）
     */
    List<UmsAdminLoginLog> listLoginLogsForExport(LoginLogQueryDTO queryDTO);

    /**
     * 清理指定时间之前的登录日志
     *
     * @param beforeTime 截止时间（<= beforeTime 的日志会被删除）
     * @return 删除条数
     */
    int cleanLoginLogsBefore(java.time.LocalDateTime beforeTime);

}
