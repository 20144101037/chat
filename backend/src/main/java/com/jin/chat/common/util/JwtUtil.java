package com.jin.chat.common.util;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.service.SysConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * <p>
 * JWT 令牌工具：签发与解析。令牌有效期从全局配置 jwt.expire-minutes 读取，默认 120 分钟（2 小时）。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
public class JwtUtil {

    /** 令牌有效期配置键 */
    public static final String CONFIG_KEY_EXPIRE_MINUTES = "jwt.expire-minutes";

    /** 默认有效期：120 分钟（2 小时） */
    public static final int DEFAULT_EXPIRE_MINUTES = 120;

    @Value("${chat.jwt.secret}")
    private String secret;

    private final SysConfigService sysConfigService;

    private SecretKey key;

    public JwtUtil(@Lazy SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(LoginUser user) {
        long expireMinutes = sysConfigService.getInt(CONFIG_KEY_EXPIRE_MINUTES, DEFAULT_EXPIRE_MINUTES);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMinutes * 60 * 1000);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，非法或过期返回 null。
     */
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(claims.getSubject()));
            user.setUsername(claims.get("username", String.class));
            user.setRole(claims.get("role", String.class));
            user.setIssuedAt(claims.getIssuedAt() == null ? null : claims.getIssuedAt().getTime());
            return user;
        } catch (Exception e) {
            log.warn("解析 token 失败: {}", e.getMessage());
            return null;
        }
    }
}
