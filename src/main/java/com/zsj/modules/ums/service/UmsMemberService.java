package com.zsj.modules.ums.service;

import com.zsj.modules.ums.dto.MemberInfoDTO;
import com.zsj.modules.ums.model.UmsMember;

/**
 * 买家用户业务接口
 */
public interface UmsMemberService {

    /**
     * 根据用户名查询买家
     */
    UmsMember getByUsername(String username);

    /**
     * 买家注册
     * @return 新买家ID
     */
    Long register(String username, String password);

    /**
     * 买家登录校验
     * @return 买家脱敏信息
     */
    MemberInfoDTO login(String username, String password);

    /**
     * 获取买家脱敏信息
     */
    MemberInfoDTO getMemberInfo(String username);
}
