package com.zsj.common.exception;

import com.zsj.common.api.CommonResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.zsj.common.exception.ApiException;


/**
 * 全局异常处理器：
 * 统一拦截 Controller 层抛出的异常，避免返回默认 Whitelabel 错误页
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常（@Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<String> handleValidException(MethodArgumentNotValidException e) {
        // 取第一个字段错误信息，便于前端直接展示
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数验证失败";
        return CommonResult.validateFailed(message);
    }

    /**
     * 处理业务异常（Service 主动抛出的可预期异常）
     * 例如：用户名已存在、用户不存在、账号被禁用
     */
    @ExceptionHandler(ApiException.class)
    public CommonResult<String> handleApiException(ApiException e) {
        return CommonResult.failed(e.getErrorCode());
    }



    /**
     * 兜底异常处理：未被其他处理器捕获的异常都会走这里
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<String> handleException(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "服务器内部错误";
        }
        return CommonResult.failed(message);
    }
}
