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
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：
 * 负责生成 token、解析 token、判断是否过期。
 */
@Component
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_MEMBER = "MEMBER";
    private static final String CLAIM_USER_TYPE = "userType";


    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 由配置密钥生成签名 Key（HS512）
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        // 保持兼容：旧调用不带 userType
        return generateToken(username, null);
    }

    /**
     * 根据用户名和用户类型生成 JWT
     */
    public String generateToken(String username, String userType) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);

        Map<String, Object> claims = new HashMap<>();
        if (userType != null && !userType.isBlank()) {
            claims.put(CLAIM_USER_TYPE, userType);
        }

        return Jwts.builder()
                .setClaims(claims)
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
     * 从 token 中解析用户类型（ADMIN/MEMBER）
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object value = claims.get(CLAIM_USER_TYPE);
        return value == null ? null : String.valueOf(value);
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

    /**
     * 判断 token 是否可以刷新：
     * 当前最小方案：只要 token 结构合法且未过期，就允许刷新。
     */
    public boolean canRefresh(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && !isTokenExpired(token);
    }

    /**
     * 刷新 token：
     * 从旧 token 取出用户名，重新签发一个新 token。
     */
    public String refreshToken(String oldToken) {
        if (!canRefresh(oldToken)) {
            return null;
        }
        String username = getUserNameFromToken(oldToken);
        if (username == null) {
            return null;
        }

        String userType = getUserTypeFromToken(oldToken);
        return generateToken(username, userType);
    }



    /**
     * 获取 token 过期时间戳（毫秒）
     */
    public Long getExpireAtMillis(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null || claims.getExpiration() == null) {
            return null;
        }
        return claims.getExpiration().getTime();
    }


}
