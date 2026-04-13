package com.zsj.modules.ums.enums;

import com.zsj.common.api.IErrorCode;

/**
 * UMS 模块业务错误码
 */
public enum UmsErrorCode implements IErrorCode {

    ADMIN_DISABLED(40002, "账号已被禁用"),
    PASSWORD_ERROR(40003, "密码错误"),
    ADMIN_NOT_FOUND(40404, "用户不存在"),
    USERNAME_EXISTS(40001, "用户名已存在");


    private final long code;
    private final String message;

    UmsErrorCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public long getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
