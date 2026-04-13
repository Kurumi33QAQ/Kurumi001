package com.zsj.modules.ums.controller;

import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.dto.AdminInfoDTO;
import com.zsj.modules.ums.dto.LoginDTO;
import com.zsj.modules.ums.dto.LoginResponseDTO;
import com.zsj.modules.ums.dto.RegisterDTO;
import com.zsj.modules.ums.enums.UmsErrorCode;
import com.zsj.security.config.JwtProperties;
import com.zsj.security.util.JwtTokenUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.zsj.modules.ums.service.UmsAdminService;
import com.zsj.modules.ums.model.UmsAdmin;
import java.util.List;


@RestController
@RequiredArgsConstructor
public class DemoController {

    private final UmsAdminService umsAdminService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;




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
    public CommonResult<LoginResponseDTO> simpleLogin(@Valid @RequestBody LoginDTO dto) {
        AdminInfoDTO adminInfo = umsAdminService.login(dto.getUsername(), dto.getPassword());

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


}
