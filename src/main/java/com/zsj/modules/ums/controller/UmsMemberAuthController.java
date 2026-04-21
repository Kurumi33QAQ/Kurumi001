package com.zsj.modules.ums.controller;

import com.zsj.common.api.CommonResult;
import com.zsj.modules.ums.dto.MemberInfoDTO;
import com.zsj.modules.ums.dto.MemberLoginDTO;
import com.zsj.modules.ums.dto.MemberLoginResponseDTO;
import com.zsj.modules.ums.dto.MemberRegisterDTO;
import com.zsj.modules.ums.service.UmsMemberService;
import com.zsj.security.config.JwtProperties;
import com.zsj.security.util.JwtTokenUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zsj.security.component.TokenBlacklistService;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.HashMap;
import java.util.Map;


/**
 * 买家认证接口
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/member/auth")
public class UmsMemberAuthController {

    private final UmsMemberService umsMemberService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 买家注册
     */
    @PostMapping("/register")
    public CommonResult<Long> register(@Valid @RequestBody MemberRegisterDTO dto) {
        Long id = umsMemberService.register(dto.getUsername(), dto.getPassword());
        return CommonResult.success(id, "买家注册成功");
    }

    /**
     * 买家登录
     */
    @PostMapping("/login")
    public CommonResult<MemberLoginResponseDTO> login(@Valid @RequestBody MemberLoginDTO dto) {
        MemberInfoDTO memberInfo = umsMemberService.login(dto.getUsername(), dto.getPassword());

        String token = jwtTokenUtil.generateToken(memberInfo.getUsername(), JwtTokenUtil.USER_TYPE_MEMBER);

        MemberLoginResponseDTO response = new MemberLoginResponseDTO();
        response.setToken(token);
        response.setTokenHead(jwtProperties.getTokenHead() + " ");
        response.setMemberInfo(memberInfo);
        return CommonResult.success(response, "买家登录成功");
    }

    /**
     * 获取当前登录买家信息
     */
    @GetMapping("/me")
    public CommonResult<MemberInfoDTO> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return CommonResult.unauthorized(null);
        }

        MemberInfoDTO info = umsMemberService.getMemberInfo(authentication.getName());
        return CommonResult.success(info, "获取当前买家信息成功");
    }

    /**
     * 买家刷新 token
     */
    @PostMapping("/token/refresh")
    public CommonResult<Map<String, String>> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return CommonResult.unauthorized(null, "未认证（缺少或非法 Authorization 头）");
        }

        String oldToken = authorization.substring(7);

        // 仅允许 MEMBER 类型 token 刷新
        String userType = jwtTokenUtil.getUserTypeFromToken(oldToken);
        if (!JwtTokenUtil.USER_TYPE_MEMBER.equals(userType)) {
            return CommonResult.unauthorized(null, "token 类型不匹配");
        }

        String newToken = jwtTokenUtil.refreshToken(oldToken);
        if (newToken == null) {
            return CommonResult.unauthorized(null, "token 无法刷新（可能已过期或无效）");
        }

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", newToken);
        tokenMap.put("tokenHead", "Bearer");
        return CommonResult.success(tokenMap, "买家刷新 token 成功");
    }

    /**
     * 买家退出登录
     */
    @PostMapping("/logout")
    public CommonResult<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return CommonResult.unauthorized("未认证（缺少或非法 Authorization 头）");
        }

        String token = authorization.substring(7);

        // 仅允许 MEMBER 类型 token 退出
        String userType = jwtTokenUtil.getUserTypeFromToken(token);
        if (!JwtTokenUtil.USER_TYPE_MEMBER.equals(userType)) {
            return CommonResult.unauthorized("token 类型不匹配");
        }

        Long expireAtMillis = jwtTokenUtil.getExpireAtMillis(token);
        if (expireAtMillis == null) {
            return CommonResult.unauthorized("token 无效，退出失败");
        }

        tokenBlacklistService.add(token, expireAtMillis);
        return CommonResult.success("买家退出登录成功");
    }

}
