package com.zsj.common.api;

/**
 * 错误码抽象接口：
 * 用统一结构描述业务状态码和提示信息
 */
public interface IErrorCode {
    long getCode();

    String getMessage();
}
