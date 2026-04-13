package com.zsj.modules.ums.service.impl;

import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.ums.dto.AdminInfoDTO;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.mapper.UmsAdminMapper;
import com.zsj.modules.ums.model.UmsAdmin;
import com.zsj.modules.ums.service.UmsAdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;


import java.util.List;

/**
 * 后台用户业务实现
 */
@Service
public class UmsAdminServiceImpl implements UmsAdminService {

    private final UmsAdminMapper umsAdminMapper;
    private final PasswordEncoder passwordEncoder;

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


    //    public UmsAdminServiceImpl(UmsAdminMapper umsAdminMapper) {
//        this.umsAdminMapper = umsAdminMapper;
//    }
    public UmsAdminServiceImpl(UmsAdminMapper umsAdminMapper, PasswordEncoder passwordEncoder) {
        this.umsAdminMapper = umsAdminMapper;
        this.passwordEncoder = passwordEncoder;
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
    public AdminInfoDTO login(String username, String password) {
        // 1. 按用户名查询用户
        UmsAdmin admin = getByUsername(username);
        if (admin == null) {
            throw new ApiException(UmsErrorCode.ADMIN_NOT_FOUND);
        }

        // 2. 校验账号状态（0=禁用）
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new ApiException(UmsErrorCode.ADMIN_DISABLED);
        }

        // 3. 临时明文校验密码（下一步升级为 BCrypt）
//        if (admin.getPassword() == null || !admin.getPassword().equals(password)) {
//            return null;
//        }
        if (admin.getPassword() == null || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new ApiException(UmsErrorCode.PASSWORD_ERROR);
        }

        // 登录成功后更新最近登录时间
        UmsAdmin update = new UmsAdmin();
        update.setId(admin.getId());
        update.setLoginTime(java.time.LocalDateTime.now());
        umsAdminMapper.updateById(update);

        // 组装脱敏返回对象（不返回密码）
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

}
