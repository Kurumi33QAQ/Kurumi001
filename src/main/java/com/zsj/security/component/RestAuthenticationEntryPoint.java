package com.zsj.security.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.common.api.CommonResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证处理器：
 * 当请求未携带有效认证信息时，返回统一 JSON。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        //设置响应编码和类型
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        //设置 HTTP 状态码
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //返回统一业务体
        CommonResult<String> result = CommonResult.unauthorized("未认证（未登录或token无效）");
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }
}
