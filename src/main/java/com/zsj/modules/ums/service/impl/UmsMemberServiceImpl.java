package com.zsj.modules.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
     * 下一步再实现登录
     */
    @Override
    public MemberInfoDTO login(String username, String password) {
        throw new UnsupportedOperationException("TODO: 下一步实现买家登录");
    }

    /**
     * 下一步再实现查询当前买家信息
     */
    @Override
    public MemberInfoDTO getMemberInfo(String username) {
        throw new UnsupportedOperationException("TODO: 下一步实现买家信息查询");
    }
}
