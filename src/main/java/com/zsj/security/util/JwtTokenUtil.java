package com.zsj.security.util;

import com.zsj.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：
 * 负责生成 token、解析 token、判断是否过期。
 */
@Component
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;

    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 由配置密钥生成签名 Key（HS512）
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据用户名生成 JWT
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(getSignKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从 token 中解析用户名（subject）
     */
    public String getUserNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims == null ? null : claims.getSubject();
    }

    /**
     * 判断 token 是否过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims == null
                || claims.getExpiration() == null
                || claims.getExpiration().before(new Date());
    }

    /**
     * 验证 token 与用户名是否匹配且未过期
     */
    public boolean validateToken(String token, String username) {
        String tokenUsername = getUserNameFromToken(token);
        return tokenUsername != null
                && tokenUsername.equals(username)
                && !isTokenExpired(token);
    }

    /**
     * 解析 token 的 claims
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
