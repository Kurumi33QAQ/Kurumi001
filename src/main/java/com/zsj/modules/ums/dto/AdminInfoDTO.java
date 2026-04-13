package com.zsj.modules.ums.dto;

/**
 * 登录成功后返回的用户信息（脱敏，不包含密码）
 */
public class AdminInfoDTO {

    private Long id;
    private String username;
    private String nickName;
    private String email;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
