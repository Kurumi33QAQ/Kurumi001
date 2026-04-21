package com.zsj.modules.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zsj.common.api.ResultCode;
import com.zsj.common.exception.ApiException;
import com.zsj.modules.ums.dto.MemberInfoDTO;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.mapper.UmsMemberMapper;
import com.zsj.modules.ums.model.UmsMember;
import com.zsj.modules.ums.service.UmsMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 买家用户业务实现
 */
@RequiredArgsConstructor
@Service
public class UmsMemberServiceImpl implements UmsMemberService {

    private final UmsMemberMapper umsMemberMapper;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final long LOCK_MINUTES = 10;


    /**
     * 根据用户名查询买家（仅查未删除）
     */
    @Override
    public UmsMember getByUsername(String username) {
        LambdaQueryWrapper<UmsMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsMember::getUsername, username)
                .eq(UmsMember::getDeleteStatus, 0);
        return umsMemberMapper.selectOne(wrapper);
    }

    /**
     * 买家注册
     */
    @Override
    public Long register(String username, String password) {
        // 1. 用户名查重
        UmsMember exist = getByUsername(username);
        if (exist != null) {
            throw new ApiException(UmsErrorCode.MEMBER_USERNAME_EXISTS);
        }

        // 2. 构建买家对象并加密密码
        UmsMember member = new UmsMember();
        member.setUsername(username);
        member.setPassword(passwordEncoder.encode(password));
        member.setNickName(username);
        member.setStatus(1);
        member.setDeleteStatus(0);
        member.setCreateTime(LocalDateTime.now());

        // 3. 落库
        int rows = umsMemberMapper.insert(member);
        if (rows <= 0) {
            throw new ApiException(ResultCode.FAILED);
        }

        return member.getId();
    }

    /**
     * 买家登录校验
     */
    @Override
    public MemberInfoDTO login(String username, String password) {
        UmsMember member = getByUsername(username);
        if (member == null) {
            throw new ApiException(UmsErrorCode.MEMBER_NOT_FOUND);
        }

        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new ApiException(UmsErrorCode.MEMBER_DISABLED);
        }

        if (member.getLockExpireTime() != null
                && member.getLockExpireTime().isAfter(LocalDateTime.now())) {
            throw new ApiException(UmsErrorCode.MEMBER_LOCKED);
        }

        // 密码错误：失败次数+1，达到阈值则锁定
        if (member.getPassword() == null || !passwordEncoder.matches(password, member.getPassword())) {
            int failCount = (member.getLoginFailCount() == null ? 0 : member.getLoginFailCount()) + 1;

            UmsMember updateFail = new UmsMember();
            updateFail.setId(member.getId());
            updateFail.setLoginFailCount(failCount);
            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                updateFail.setLockExpireTime(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            umsMemberMapper.updateById(updateFail);

            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                throw new ApiException(UmsErrorCode.MEMBER_LOCKED_BY_FAIL);
            }
            throw new ApiException(UmsErrorCode.MEMBER_PASSWORD_ERROR);
        }

        // 密码正确：清失败次数、清锁定时间、更新登录时间
        LambdaUpdateWrapper<UmsMember> successWrapper = new LambdaUpdateWrapper<>();
        successWrapper.eq(UmsMember::getId, member.getId())
                .set(UmsMember::getLoginTime, LocalDateTime.now())
                .set(UmsMember::getLoginFailCount, 0)
                .set(UmsMember::getLockExpireTime, null);
        umsMemberMapper.update(null, successWrapper);

        MemberInfoDTO dto = new MemberInfoDTO();
        dto.setId(member.getId());
        dto.setUsername(member.getUsername());
        dto.setNickName(member.getNickName());
        dto.setPhone(member.getPhone());
        dto.setEmail(member.getEmail());
        dto.setStatus(member.getStatus());
        return dto;
    }


    /**
     * 获取买家脱敏信息
     */
    @Override
    public MemberInfoDTO getMemberInfo(String username) {
        UmsMember member = getByUsername(username);
        if (member == null) {
            throw new ApiException(UmsErrorCode.MEMBER_NOT_FOUND);
        }

        MemberInfoDTO dto = new MemberInfoDTO();
        dto.setId(member.getId());
        dto.setUsername(member.getUsername());
        dto.setNickName(member.getNickName());
        dto.setPhone(member.getPhone());
        dto.setEmail(member.getEmail());
        dto.setStatus(member.getStatus());
        return dto;
    }

}
