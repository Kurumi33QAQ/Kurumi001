package com.zsj.modules.ums.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.dto.*;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.modules.ums.model.UmsAdminLoginLog;
import com.zsj.security.component.TokenBlacklistService;
import com.zsj.security.config.JwtProperties;
import com.zsj.security.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.zsj.modules.ums.service.UmsAdminService;
import com.zsj.modules.ums.model.UmsAdmin;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;


@Validated
@RestController
@RequiredArgsConstructor
public class DemoController {

    private final UmsAdminService umsAdminService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;



    /**
     * 成功示例：验证 CommonResult.success(...)
     */
    @GetMapping("/demo/success")
    public CommonResult<String> success() {
        return CommonResult.success("hello mall-learning", "demo success");
    }

    /**
     * 失败示例：验证 CommonResult.failed(...)
     */
    @GetMapping("/demo/fail")
    public CommonResult<String> fail() {
        return CommonResult.failed("demo failed");
    }


    @GetMapping("/demo/error")
    public CommonResult<String> error() {
        throw new RuntimeException("demo runtime exception");
    }


    @PostMapping("/demo/login")
    public CommonResult<String> login(@Valid @RequestBody LoginDTO dto) {
        return CommonResult.success("login param valid");
    }


    @GetMapping("/demo/admin/list")
    public CommonResult<List<UmsAdmin>> listAdmin() {
        List<UmsAdmin> list = umsAdminService.listAll();
        return CommonResult.success(list);
    }


    @GetMapping("/demo/admin/{username}")
    public CommonResult<UmsAdmin> getAdminByUsername(@PathVariable String username) {
        UmsAdmin admin = umsAdminService.getByUsername(username);
        if (admin == null) {
            return CommonResult.failed(UmsErrorCode.ADMIN_NOT_FOUND);
        }
        return CommonResult.success(admin);
    }


    /**
     * 简化登录：校验通过后返回 token + tokenHead + 用户信息
     */
    @PostMapping("/demo/login/simple")
    public CommonResult<LoginResponseDTO> simpleLogin(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        AdminInfoDTO adminInfo = umsAdminService.login(dto.getUsername(), dto.getPassword(), ip, userAgent);

        String token = jwtTokenUtil.generateToken(adminInfo.getUsername());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setTokenHead(jwtProperties.getTokenHead() + " ");
        response.setAdminInfo(adminInfo);

        return CommonResult.success(response, "登录成功");
    }





    //明文转换成密文，调试时使用，实际生产中不要暴露这个接口
//    @GetMapping("/demo/pwd/encode")
//    public CommonResult<String> encode(@RequestParam String raw) {
//        return CommonResult.success(passwordEncoder.encode(raw), "密码加密成功");
//    }


    @PostMapping("/demo/register/simple")
    public CommonResult<Long> simpleRegister(@Valid @RequestBody RegisterDTO dto) {
        Long id = umsAdminService.register(dto.getUsername(), dto.getPassword());
        return CommonResult.success(id, "注册成功");
    }


    /**
     * 获取当前登录用户信息（不再信任前端传用户名）
     */
    @GetMapping("/demo/admin/me")
    public CommonResult<AdminInfoDTO> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        String username = authentication.getName();
        AdminInfoDTO info = umsAdminService.getAdminInfo(username);
        return CommonResult.success(info, "获取当前用户信息成功");
    }




    /**
     * 演示：解析并校验 token
     */
    @GetMapping("/demo/token/check")
    public CommonResult<String> checkToken(@RequestParam String token, @RequestParam String username) {
        String tokenUsername = jwtTokenUtil.getUserNameFromToken(token);
        boolean valid = jwtTokenUtil.validateToken(token, username);

        if (!valid) {
            return CommonResult.failed("token无效或已过期");
        }
        return CommonResult.success("token有效，解析用户名：" + tokenUsername);
    }



    /**
     * 需要 admin:read 权限的测试接口
     */
    @PreAuthorize("hasAuthority('admin:read')")
    @GetMapping("/demo/admin/secure")
    public CommonResult<String> secure() {
        return CommonResult.success("你有 admin:read 权限");
    }


    /**
     * 分页查询登录日志
     */
    @GetMapping("/demo/admin/login-log/list")
    public CommonResult<IPage<UmsAdminLoginLog>> listLoginLogs(@Valid @ModelAttribute LoginLogQueryDTO queryDTO) {
        IPage<UmsAdminLoginLog> pageResult = umsAdminService.pageLoginLogs(queryDTO);
        return CommonResult.success(pageResult, "获取登录日志分页数据成功");
    }


    /**
     * 导出登录日志（CSV）
     */
    @GetMapping("/demo/admin/login-log/export")
    public void exportLoginLogs(@ModelAttribute LoginLogQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<UmsAdminLoginLog> logs = umsAdminService.listLoginLogsForExport(queryDTO);

        // 设置响应头：告诉浏览器这是一个下载文件
        String fileName = URLEncoder.encode("login_logs.csv", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        // 写 BOM，避免 Excel 打开中文乱码
        response.getWriter().write("\uFEFF");

        // CSV 表头
        response.getWriter().write("ID,管理员ID,用户名,IP,客户端,状态,消息,创建时间\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // CSV 数据行
        for (UmsAdminLoginLog log : logs) {
            String statusText = (log.getStatus() != null && log.getStatus() == 1) ? "成功" : "失败";
            String createTime = log.getCreateTime() == null ? "" : log.getCreateTime().format(formatter);

            // 处理逗号和双引号，避免破坏 CSV 格式
            String username = escapeCsv(log.getUsername());
            String ip = escapeCsv(log.getIp());
            String userAgent = escapeCsv(log.getUserAgent());
            String message = escapeCsv(log.getMessage());

            response.getWriter().write(String.format(
                    "%d,%d,%s,%s,%s,%s,%s,%s\n",
                    log.getId() == null ? 0 : log.getId(),
                    log.getAdminId() == null ? 0 : log.getAdminId(),
                    username,
                    ip,
                    userAgent,
                    statusText,
                    message,
                    createTime
            ));
        }

        response.getWriter().flush();
    }

    /**
     * CSV 字段转义：包含逗号/双引号/换行时用双引号包裹，并转义内部双引号
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }



    /**
     * 清理指定时间之前的登录日志
     */
    @PostMapping("/demo/admin/login-log/clean")
    public CommonResult<Integer> cleanLoginLogs(@Valid @RequestBody LoginLogCleanDTO cleanDTO) {
        int count = umsAdminService.cleanLoginLogsBefore(cleanDTO.getBeforeTime());
        return CommonResult.success(count, "清理登录日志成功");
    }



    /**
     * 刷新 token：
     * 1. 从 Authorization 头读取旧 token
     * 2. 校验是否可刷新
     * 3. 返回新 token
     */
    @PostMapping("/demo/token/refresh")
    public CommonResult<java.util.Map<String, String>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        // 1) 基础校验：请求头不能为空，且要以 "Bearer " 开头
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return CommonResult.unauthorized(null, "未认证（缺少或非法 Authorization 头）");
        }

        // 2) 提取旧 token
        String oldToken = authorization.substring(7);

        // 3) 调用工具类刷新
        String newToken = jwtTokenUtil.refreshToken(oldToken);
        if (newToken == null) {
            return CommonResult.unauthorized(null, "token 无法刷新（可能已过期或无效）");
        }

        // 4) 按现有登录返回风格返回 token
        java.util.Map<String, String> tokenMap = new java.util.HashMap<>();
        tokenMap.put("token", newToken);
        tokenMap.put("tokenHead", "Bearer");

        return CommonResult.success(tokenMap, "刷新 token 成功");
    }



    /**
     * 退出登录：
     * 1. 读取当前 Authorization 头
     * 2. 解析 token 过期时间
     * 3. 把 token 加入黑名单，立即失效
     */
    @PostMapping("/demo/logout")
    public CommonResult<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return CommonResult.unauthorized("未认证（缺少或非法 Authorization 头）");
        }

        String token = authorization.substring(7);
        Long expireAtMillis = jwtTokenUtil.getExpireAtMillis(token);

        // token 无法解析时，也按未认证处理
        if (expireAtMillis == null) {
            return CommonResult.unauthorized("token 无效，退出失败");
        }

        tokenBlacklistService.add(token, expireAtMillis);
        return CommonResult.success("退出登录成功");
    }



    /**
     * 分配用户角色（覆盖式）
     */
    @PostMapping("/demo/admin/role/assign")
    public CommonResult<String> assignAdminRoles(@Valid @RequestBody AdminRoleAssignDTO dto) {
        umsAdminService.assignRoles(dto.getAdminId(), dto.getRoleIds());
        return CommonResult.success("分配角色成功");
    }



    /**
     * 查询用户已分配角色ID列表
     */
    @GetMapping("/demo/admin/role/list")
    public CommonResult<java.util.List<Long>> listAdminRoles(@RequestParam Long adminId) {
        java.util.List<Long> roleIds = umsAdminService.getRoleIdsByAdminId(adminId);
        return CommonResult.success(roleIds, "查询用户角色成功");
    }



    /**
     * 给角色分配资源（覆盖式）
     */
    @PostMapping("/demo/admin/role/resource/assign")
    public CommonResult<String> assignRoleResources(@Valid @RequestBody RoleResourceAssignDTO dto) {
        umsAdminService.assignRoleResources(dto.getRoleId(), dto.getResourceIds());
        return CommonResult.success("角色资源分配成功");
    }
}
