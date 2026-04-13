package com.zsj.modules.ums.dto;

/**
 * 登录响应对象：
 * 返回 token 信息 + 当前登录用户信息（脱敏）
 */
public class LoginResponseDTO {

    /**
     * JWT 令牌
     */
    private String token;

    /**
     * token 前缀（如 Bearer）
     */
    private String tokenHead;

    /**
     * 当前用户信息（脱敏）
     */
    private AdminInfoDTO adminInfo;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenHead() { return tokenHead; }
    public void setTokenHead(String tokenHead) { this.tokenHead = tokenHead; }

    public AdminInfoDTO getAdminInfo() { return adminInfo; }
    public void setAdminInfo(AdminInfoDTO adminInfo) { this.adminInfo = adminInfo; }
}
