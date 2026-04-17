package com.zsj.modules.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.ums.dto.AdminInfoDTO;
import com.zsj.modules.ums.dto.LoginLogQueryDTO;
import com.zsj.modules.ums.dto.OptionDTO;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.mapper.UmsAdminLoginLogMapper;
import com.zsj.modules.ums.mapper.UmsAdminMapper;
import com.zsj.modules.ums.mapper.UmsAdminRoleRelationMapper;
import com.zsj.modules.ums.mapper.UmsRoleResourceRelationMapper;
import com.zsj.modules.ums.model.UmsAdmin;
import com.zsj.modules.ums.model.UmsAdminLoginLog;
import com.zsj.modules.ums.model.UmsAdminRoleRelation;
import com.zsj.modules.ums.model.UmsRoleResourceRelation;
import com.zsj.modules.ums.service.UmsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.zsj.modules.ums.dto.AdminPermissionSummaryDTO;
import com.zsj.modules.ums.model.UmsAdminRoleRelation;
import com.zsj.modules.ums.model.UmsRole;



import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 后台用户业务实现
 */
@RequiredArgsConstructor
@Service
public class UmsAdminServiceImpl implements UmsAdminService {

    private final UmsAdminMapper umsAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final UmsAdminLoginLogMapper umsAdminLoginLogMapper;
    // Redis 权限缓存 key 前缀
    private static final String AUTH_CACHE_PREFIX = "security:authority:user:";
    // 权限缓存过期时间（先用30分钟）
    private static final long AUTH_CACHE_TTL_MINUTES = 30;
    private final StringRedisTemplate stringRedisTemplate;
    private final UmsAdminRoleRelationMapper umsAdminRoleRelationMapper;
    private final UmsRoleResourceRelationMapper umsRoleResourceRelationMapper;
    /**
     * 登录失败达到该次数后锁定
     */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    /**
     * 锁定时长（分钟）
     */
    private static final long LOCK_MINUTES = 10;





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

        // 1) 先判断是否锁定中
        if (isLocked(admin)) {
            long remain = remainLockMinutes(admin);
            recordLoginLog(admin.getId(), username, ip, userAgent, 0, "账号锁定中");
            throw new ApiException(UmsErrorCode.ADMIN_LOCKED);
        }

        // 2) 密码错误：失败次数+1；达到阈值就锁定
        if (admin.getPassword() == null || !passwordEncoder.matches(password, admin.getPassword())) {
            int failCount = (admin.getLoginFailCount() == null ? 0 : admin.getLoginFailCount()) + 1;

            UmsAdmin updateFail = new UmsAdmin();
            updateFail.setId(admin.getId());
            updateFail.setLoginFailCount(failCount);

            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                updateFail.setLockExpireTime(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }

            umsAdminMapper.updateById(updateFail);

            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                recordLoginLog(admin.getId(), username, ip, userAgent, 0, "密码错误，账号已锁定");
                throw new ApiException(UmsErrorCode.ADMIN_LOCKED_BY_FAIL);
            } else {
                recordLoginLog(admin.getId(), username, ip, userAgent, 0, "密码错误");
                throw new ApiException(UmsErrorCode.PASSWORD_ERROR);
            }
        }

        // 3) 密码正确：清零失败次数、清空锁定时间、更新登录时间
        LambdaUpdateWrapper<UmsAdmin> successWrapper = new LambdaUpdateWrapper<>();
        successWrapper.eq(UmsAdmin::getId, admin.getId())
                .set(UmsAdmin::getLoginTime, LocalDateTime.now())
                .set(UmsAdmin::getLoginFailCount, 0)
                .set(UmsAdmin::getLockExpireTime, null);

        umsAdminMapper.update(null, successWrapper);


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
        evictAuthorityCache(username);

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
     * 获取用户权限列表（Redis缓存版）
     * 1. 先查 Redis
     * 2. 命中直接返回
     * 3. 未命中查数据库并回写 Redis（带TTL）
     */
    @Override
    public List<String> getAuthorityList(String username) {
        String key = AUTH_CACHE_PREFIX + username;

        // 1) 先从 Redis 取缓存（Set结构）
        Set<String> cachedSet = stringRedisTemplate.opsForSet().members(key);
        if (cachedSet != null && !cachedSet.isEmpty()) {
            return new ArrayList<>(cachedSet);
        }

        // 2) 缓存未命中，查数据库
        List<String> authorityList = umsAdminMapper.getResourceNameListByUsername(username);

        // 3) 回写缓存
        if (authorityList != null && !authorityList.isEmpty()) {
            stringRedisTemplate.opsForSet().add(key, authorityList.toArray(new String[0]));
            stringRedisTemplate.expire(key, AUTH_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return authorityList == null ? new ArrayList<>() : authorityList;
    }


    /**
     * 清理指定用户权限缓存（Redis）
     */
    @Override
    public void evictAuthorityCache(String username) {
        String key = AUTH_CACHE_PREFIX + username;
        stringRedisTemplate.delete(key);
    }


    /**
     * 获取当前缓存条目数（调试用）
     */
    @Override
    public int getAuthorityCacheSize() {
        Set<String> keys = stringRedisTemplate.keys(AUTH_CACHE_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }



    /**
     * 分页查询登录日志（支持用户名、状态、时间区间筛选）
     */
    @Override
    public IPage<UmsAdminLoginLog> pageLoginLogs(LoginLogQueryDTO queryDTO) {
        LambdaQueryWrapper<UmsAdminLoginLog> wrapper = new LambdaQueryWrapper<>();

        // 用户名：模糊匹配
        if (StringUtils.hasText(queryDTO.getUsername())) {
            wrapper.like(UmsAdminLoginLog::getUsername, queryDTO.getUsername());
        }

        // 状态：精确匹配（0失败，1成功）
        if (queryDTO.getStatus() != null) {
            wrapper.eq(UmsAdminLoginLog::getStatus, queryDTO.getStatus());
        }

        // 时间区间：大于等于开始时间，小于等于结束时间
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(UmsAdminLoginLog::getCreateTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(UmsAdminLoginLog::getCreateTime, queryDTO.getEndTime());
        }

        // 按创建时间倒序，再按id倒序（保证同秒下顺序稳定）
        wrapper.orderByDesc(UmsAdminLoginLog::getCreateTime)
                .orderByDesc(UmsAdminLoginLog::getId);

        Page<UmsAdminLoginLog> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return umsAdminLoginLogMapper.selectPage(page, wrapper);
    }


    /**
     * 按条件查询登录日志（用于导出，不分页）
     */
    @Override
    public List<UmsAdminLoginLog> listLoginLogsForExport(LoginLogQueryDTO queryDTO) {
        LambdaQueryWrapper<UmsAdminLoginLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getUsername())) {
            wrapper.like(UmsAdminLoginLog::getUsername, queryDTO.getUsername());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(UmsAdminLoginLog::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(UmsAdminLoginLog::getCreateTime, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(UmsAdminLoginLog::getCreateTime, queryDTO.getEndTime());
        }

        wrapper.orderByDesc(UmsAdminLoginLog::getCreateTime)
                .orderByDesc(UmsAdminLoginLog::getId);

        return umsAdminLoginLogMapper.selectList(wrapper);
    }


    /**
     * 清理指定时间之前的登录日志
     */
    @Override
    public int cleanLoginLogsBefore(LocalDateTime beforeTime) {
        LambdaQueryWrapper<UmsAdminLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(UmsAdminLoginLog::getCreateTime, beforeTime);
        return umsAdminLoginLogMapper.delete(wrapper);
    }



    /**
     * 给用户分配角色（覆盖式）：
     * 1. 删除该用户旧角色关系
     * 2. 插入新角色关系
     * 3. 清理该用户权限缓存，保证下次鉴权读取最新权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long adminId, List<Long> roleIds) {
        // 1) 校验用户存在
        UmsAdmin admin = umsAdminMapper.selectById(adminId);
        if (admin == null) {
            throw new ApiException(UmsErrorCode.ADMIN_NOT_FOUND);
        }

        // 2) 删旧关系
        LambdaQueryWrapper<UmsAdminRoleRelation> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UmsAdminRoleRelation::getAdminId, adminId);
        umsAdminRoleRelationMapper.delete(deleteWrapper);

        // 3) 插入新关系
        for (Long roleId : roleIds) {
            UmsAdminRoleRelation relation = new UmsAdminRoleRelation();
            relation.setAdminId(adminId);
            relation.setRoleId(roleId);
            umsAdminRoleRelationMapper.insert(relation);
        }

        // 4) 自动清理权限缓存
        evictAuthorityCache(admin.getUsername());
    }



    /**
     * 查询用户已分配角色ID列表
     */
    @Override
    public List<Long> getRoleIdsByAdminId(Long adminId) {
        LambdaQueryWrapper<UmsAdminRoleRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsAdminRoleRelation::getAdminId, adminId);

        List<UmsAdminRoleRelation> list = umsAdminRoleRelationMapper.selectList(wrapper);
        return list.stream().map(UmsAdminRoleRelation::getRoleId).toList();
    }



    /**
     * 给角色分配资源（覆盖式）：
     * 1. 删除该角色旧资源关系
     * 2. 插入新资源关系
     * 3. 清理所有受影响用户的权限缓存（拥有该角色的用户）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleResources(Long roleId, List<Long> resourceIds) {
        // 1) 删除该角色旧资源关系
        LambdaQueryWrapper<UmsRoleResourceRelation> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(UmsRoleResourceRelation::getRoleId, roleId);
        umsRoleResourceRelationMapper.delete(delWrapper);

        // 2) 插入新资源关系
        for (Long resourceId : resourceIds) {
            UmsRoleResourceRelation relation = new UmsRoleResourceRelation();
            relation.setRoleId(roleId);
            relation.setResourceId(resourceId);
            umsRoleResourceRelationMapper.insert(relation);
        }

        // 3) 找到拥有该角色的用户，并清理其权限缓存
        LambdaQueryWrapper<UmsAdminRoleRelation> adminRoleWrapper = new LambdaQueryWrapper<>();
        adminRoleWrapper.eq(UmsAdminRoleRelation::getRoleId, roleId);
        java.util.List<UmsAdminRoleRelation> adminRoleList = umsAdminRoleRelationMapper.selectList(adminRoleWrapper);

        for (UmsAdminRoleRelation ar : adminRoleList) {
            UmsAdmin admin = umsAdminMapper.selectById(ar.getAdminId());
            if (admin != null && admin.getUsername() != null) {
                evictAuthorityCache(admin.getUsername());
            }
        }
    }



    /**
     * 角色下拉选项
     */
    @Override
    public List<OptionDTO> listRoleOptions() {
        return umsAdminMapper.listRoleOptions();
    }

    /**
     * 资源下拉选项（权限点）
     */
    @Override
    public List<OptionDTO> listResourceOptions() {
        return umsAdminMapper.listResourceOptions();
    }


    /**
     * 查询角色已分配资源ID列表
     */
    @Override
    public List<Long> getResourceIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<UmsRoleResourceRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsRoleResourceRelation::getRoleId, roleId);

        List<UmsRoleResourceRelation> list = umsRoleResourceRelationMapper.selectList(wrapper);
        return list.stream().map(UmsRoleResourceRelation::getResourceId).toList();
    }



    /**
     * 查询用户权限汇总（用户信息 + 角色名 + 权限点）
     */
    @Override
    public AdminPermissionSummaryDTO getAdminPermissionSummary(Long adminId) {
        UmsAdmin admin = umsAdminMapper.selectById(adminId);
        if (admin == null) {
            throw new ApiException(UmsErrorCode.ADMIN_NOT_FOUND);
        }

        // 1) 查用户角色ID
        LambdaQueryWrapper<UmsAdminRoleRelation> arWrapper = new LambdaQueryWrapper<>();
        arWrapper.eq(UmsAdminRoleRelation::getAdminId, adminId);
        List<UmsAdminRoleRelation> relationList = umsAdminRoleRelationMapper.selectList(arWrapper);
        List<Long> roleIds = relationList.stream().map(UmsAdminRoleRelation::getRoleId).toList();

        // 2) 查角色名
        List<String> roleNames = new java.util.ArrayList<>();
        if (!roleIds.isEmpty()) {
            List<UmsRole> roles = umsAdminMapper.selectRolesByIds(roleIds);
            roleNames = roles.stream().map(UmsRole::getName).toList();
        }

        // 3) 查权限点（复用你现有动态权限查询）
        List<String> authorities = getAuthorityList(admin.getUsername());

        // 4) 组装返回
        AdminPermissionSummaryDTO dto = new AdminPermissionSummaryDTO();
        dto.setAdminId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setRoleNames(roleNames);
        dto.setAuthorities(authorities);
        return dto;
    }



    /**
     * 是否处于锁定中
     */
    private boolean isLocked(UmsAdmin admin) {
        return admin.getLockExpireTime() != null
                && admin.getLockExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 剩余锁定分钟数（向上取整）
     */
    private long remainLockMinutes(UmsAdmin admin) {
        if (admin.getLockExpireTime() == null) {
            return 0;
        }
        long seconds = java.time.Duration.between(LocalDateTime.now(), admin.getLockExpireTime()).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (seconds + 59) / 60;
    }


}
