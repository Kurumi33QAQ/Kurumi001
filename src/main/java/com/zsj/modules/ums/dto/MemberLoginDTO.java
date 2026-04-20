package com.zsj.modules.ums.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 买家登录请求参数
 */
@Data
public class MemberLoginDTO {

    /**
     * 买家登录用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 买家登录密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
