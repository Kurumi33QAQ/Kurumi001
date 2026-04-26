package com.zsj.modules.ums.enums;

import com.zsj.common.api.IErrorCode;

/**
 * UMS 模块业务错误码
 */
public enum UmsErrorCode implements IErrorCode {

    USERNAME_EXISTS(40001, "用户名已存在"),
    ADMIN_DISABLED(40002, "账号已被禁用"),
    PASSWORD_ERROR(40003, "密码错误"),
    ADMIN_NOT_FOUND(40404, "用户不存在"),
    ADMIN_LOCKED(40010, "账号已锁定，请稍后再试"),
    PRODUCT_PUBLISH_STATUS_INVALID(40020, "上架状态只能是0或1"),

    MEMBER_USERNAME_EXISTS(41001, "买家用户名已存在"),
    MEMBER_DISABLED(41002, "买家账号已被禁用"),
    MEMBER_PASSWORD_ERROR(41003, "买家密码错误"),
    MEMBER_NOT_FOUND(41404, "买家用户不存在"),
    MEMBER_LOCKED(41010, "买家账号已锁定，请稍后再试"),
    MEMBER_LOCKED_BY_FAIL(41011, "买家密码错误次数过多，账号已锁定10分钟"),
    ORDER_NOT_CANCELABLE(42001, "仅待支付订单可取消"),
    ORDER_SUBMIT_DUPLICATE(42002, "订单正在创建，请勿重复提交"),

    SECKILL_ACTIVITY_NOT_FOUND(43001, "秒杀活动不存在"),
    SECKILL_ACTIVITY_DISABLED(43002, "秒杀活动未启用"),
    SECKILL_NOT_STARTED(43003, "秒杀活动未开始"),
    SECKILL_ENDED(43004, "秒杀活动已结束"),
    SECKILL_STOCK_NOT_ENOUGH(43005, "秒杀库存不足"),
    SECKILL_REPEAT(43006, "不能重复秒杀同一活动"),
    SECKILL_PRODUCT_INVALID(43007, "秒杀商品不可购买"),

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
