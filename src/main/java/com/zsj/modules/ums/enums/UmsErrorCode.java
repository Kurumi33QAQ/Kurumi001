package com.zsj.modules.ums.enums;

import com.zsj.common.api.IErrorCode;

/**
 * UMS 模块业务错误码
 */
public enum UmsErrorCode implements IErrorCode {

    ADMIN_DISABLED(40002, "账号已被禁用"),
    PASSWORD_ERROR(40003, "密码错误"),
    ADMIN_NOT_FOUND(40404, "用户不存在"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    ADMIN_LOCKED(40010, "账号已锁定，请稍后再试"),
    ADMIN_LOCKED_BY_FAIL(40011, "密码错误次数过多，账号已锁定10分钟");



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
