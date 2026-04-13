package com.zsj.security.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.common.api.CommonResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 无权限处理器：
 * 当已认证但无权访问资源时，返回统一 JSON。
 */
@Component
public class RestfulAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        CommonResult<String> result = CommonResult.forbidden("无权限访问该资源");
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }
}
