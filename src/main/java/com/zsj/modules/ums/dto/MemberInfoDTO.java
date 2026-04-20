package com.zsj.modules.ums.dto;

import lombok.Data;

/**
 * 买家登录后返回的脱敏用户信息
 */
@Data
public class MemberInfoDTO {

    /**
     * 买家ID
     */
    private Long id;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;
}
