package com.zsj.common.api;

import lombok.Getter;

/**
 * 统一接口返回对象
 * @param <T> 业务数据类型（对象、列表、分页等都可以）
 */
@Getter
public class CommonResult<T> {

    /**
     * 业务状态码（由系统约定，不直接等同于 HTTP 状态码）
     */
    private final long code;

    /**
     * 提示信息（给前端或用户展示）
     */
    private final String message;

    /**
     * 业务数据载荷
     */
    private final T data;

    /**
     * 构造器用 protected：
     * 1. 限制外部随意 new，鼓励统一走静态工厂方法
     * 2. 保留同包/子类扩展能力
     */
    protected CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 统一构建方法（核心复用点）：
     * 通过 IErrorCode 读取 code/message，避免在各方法里重复写状态码逻辑
     */
    protected static <T> CommonResult<T> build(T data, IErrorCode errorCode) {
        return new CommonResult<>(errorCode.getCode(), errorCode.getMessage(), data);
    }

    /**
     * 成功返回（使用默认成功文案）
     */
    public static <T> CommonResult<T> success(T data) {
        return build(data, ResultCode.SUCCESS);
    }

    /**
     * 成功返回（允许自定义成功文案）
     */
    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回（默认失败文案）
     */
    public static <T> CommonResult<T> failed() {
        return build(null, ResultCode.FAILED);
    }

    /**
     * 失败返回（自定义失败文案）
     */
    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(ResultCode.FAILED.getCode(), message, null);
    }

    /**
     * 失败返回（支持传入模块自定义错误码）
     * 例如：UmsErrorCode.USERNAME_EXISTS
     */
    public static <T> CommonResult<T> failed(IErrorCode errorCode) {
        return build(null, errorCode);
    }

    /**
     * 参数校验失败（如 @Valid 校验不通过）
     */
    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
    }

    /**
     * 未认证（未登录或 token 无效）
     */
    public static <T> CommonResult<T> unauthorized(T data) {
        return build(data, ResultCode.UNAUTHORIZED);
    }

    public static <T> CommonResult<T> unauthorized(T data, String message) {
        return new CommonResult<>(401, message, data);
    }


    /**
     * 无权限（已登录，但没有访问该资源的权限）
     */
    public static <T> CommonResult<T> forbidden(T data) {
        return build(data, ResultCode.FORBIDDEN);
    }
}
