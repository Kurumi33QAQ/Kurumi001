package com.zsj.common.exception;

import com.zsj.common.api.IErrorCode;

/**
 * 业务异常：用于在 Service 层抛出明确业务错误
 */
public class ApiException extends RuntimeException {

    private final IErrorCode errorCode;

    public ApiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public IErrorCode getErrorCode() {
        return errorCode;
    }
}
