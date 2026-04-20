package com.zsj.modules.ums.dto;

import lombok.Data;

/**
 * 买家登录响应对象
 * 返回 token 信息 + 当前买家脱敏信息
 */
@Data
public class MemberLoginResponseDTO {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * token前缀（例如 Bearer ）
     */
    private String tokenHead;

    /**
     * 当前登录买家信息
     */
    private MemberInfoDTO memberInfo;
}
