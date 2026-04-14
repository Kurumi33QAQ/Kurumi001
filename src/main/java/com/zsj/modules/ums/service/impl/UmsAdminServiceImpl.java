package com.zsj.modules.ums.service.impl;

import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.ums.dto.AdminInfoDTO;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.mapper.UmsAdminLoginLogMapper;
import com.zsj.modules.ums.mapper.UmsAdminMapper;
import com.zsj.modules.ums.model.UmsAdmin;
import com.zsj.modules.ums.model.UmsAdminLoginLog;
import com.zsj.modules.ums.service.UmsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;


import java.util.List;

/**
 * 后台用户业务实现
 */
@RequiredArgsConstructor
@Service
public class UmsAdminServiceImpl implements UmsAdminService {

    private final UmsAdminMapper umsAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final UmsAdminLoginLogMapper umsAdminLoginLogMapper;


    /**
     * 权限缓存（key=username, value=权限列表）
     * 先用内存缓存做最小优化，后续可替换为 Redis。
     */
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.List<String>> authorityCache =
            new java.util.concurrent.ConcurrentHashMap<>();


    /**
     * 实体转脱敏DTO（统一映射，避免重复代码）
     */
    private AdminInfoDTO toAdminInfoDTO(UmsAdmin admin) {
        AdminInfoDTO dto = new AdminInfoDTO();
        dto.setId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setNickName(admin.getNickName());
        dto.setEmail(admin.getEmail());
        dto.setStatus(admin.getStatus());
        return dto;
    }

    /**
     * 记录登录审计日志
     */
    private void recordLoginLog(Long adminId, String username, String ip, String userAgent, Integer status, String message) {
        UmsAdminLoginLog log = new UmsAdminLoginLog();
        log.setAdminId(adminId);
        log.setUsername(username);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        log.setMessage(message);
        log.setCreateTime(java.time.LocalDateTime.now());
        umsAdminLoginLogMapper.insert(log);
    }




    @Override
    public List<UmsAdmin> listAll() {
        // 当前先做最小实现：查询全部用户
        return umsAdminMapper.selectList(null);
    }

    @Override
    public UmsAdmin getByUsername(String username) {
        LambdaQueryWrapper<UmsAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsAdmin::getUsername, username);
        // 查单个对象，找不到返回 null
        return umsAdminMapper.selectOne(wrapper);
    }


    /**
     * 登录校验：按用户名查用户，校验状态与密码
     */
    @Override
    public AdminInfoDTO login(String username, String password, String ip, String userAgent) {
        UmsAdmin admin = getByUsername(username);
        if (admin == null) {
            recordLoginLog(null, username, ip, userAgent, 0, "用户不存在");
            throw new ApiException(UmsErrorCode.ADMIN_NOT_FOUND);
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            recordLoginLog(admin.getId(), username, ip, userAgent, 0, "账号已禁用");
            throw new ApiException(UmsErrorCode.ADMIN_DISABLED);
        }

        if (admin.getPassword() == null || !passwordEncoder.matches(password, admin.getPassword())) {
            recordLoginLog(admin.getId(), username, ip, userAgent, 0, "密码错误");
            throw new ApiException(UmsErrorCode.PASSWORD_ERROR);
        }

        UmsAdmin update = new UmsAdmin();
        update.setId(admin.getId());
        update.setLoginTime(java.time.LocalDateTime.now());
        umsAdminMapper.updateById(update);

        recordLoginLog(admin.getId(), username, ip, userAgent, 1, "登录成功");
        return toAdminInfoDTO(admin);
    }



    /**
     * 用户注册：用户名重复则抛业务异常；成功返回新用户ID
     */
    @Override
    public Long register(String username, String password) {
        // 1. 用户名是否已存在
        UmsAdmin exist = getByUsername(username);
        if (exist != null) {
            throw new ApiException(UmsErrorCode.USERNAME_EXISTS);
        }

        // 2. 构建用户并加密密码
        UmsAdmin admin = new UmsAdmin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setStatus(1);
        admin.setCreateTime(java.time.LocalDateTime.now());

        // 3. 入库
        int rows = umsAdminMapper.insert(admin);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }


        // 注册后清理该用户权限缓存，避免后续读到旧数据
        authorityCache.remove(username);

        return admin.getId();
    }


    /**
     * 根据用户名获取脱敏用户信息
     */
    @Override
    public AdminInfoDTO getAdminInfo(String username) {
        UmsAdmin admin = getByUsername(username);
        if (admin == null) {
            throw new ApiException(UmsErrorCode.ADMIN_NOT_FOUND);
        }

        return toAdminInfoDTO(admin);

    }


    /**
     * 轻量动态权限示例：
     * - demo_user 拥有 admin:read
     * - 其他用户先给空权限
     * 后续可替换为数据库查询。
     */
//    @Override
//    public java.util.List<String> getAuthorityList(String username) {
//        if ("demo_user".equals(username)) {
//            return java.util.Collections.singletonList("admin:read");
//        }
//        return java.util.Collections.emptyList();
//    }


    /**
     * 根据用户名动态查询权限列表（来自数据库RBAC关系）
     */
//    @Override
//    public java.util.List<String> getAuthorityList(String username) {
//        return umsAdminMapper.getResourceNameListByUsername(username);
//    }


    /**
     * 根据用户名获取权限列表（带本地缓存）
     */
    @Override
    public java.util.List<String> getAuthorityList(String username) {
        return authorityCache.computeIfAbsent(username, key -> umsAdminMapper.getResourceNameListByUsername(key));
    }


    /**
     * 清理指定用户权限缓存
     */
    @Override
    public void evictAuthorityCache(String username) {
        authorityCache.remove(username);
    }

    /**
     * 获取当前缓存条目数（调试用）
     */
    @Override
    public int getAuthorityCacheSize() {
        return authorityCache.size();
    }


}
