package com.na.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
public class NaJwtUtil {

    // 生成 JWT Token
    public static String generateToken(String subject, String secret, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        // 创建 HMAC SHA256 密钥，建议 secret 长度 >= 32 字节
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(nowMillis + ttlMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 解析 JWT Token，返回 subject
    public static String parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        try {
            Jws<Claims> jwsClaims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            // 验证通过，返回 subject
            return jwsClaims.getBody().getSubject();
        } catch (JwtException e) {
            // 包括签名无效、过期等异常
            System.out.println("Invalid or expired JWT: " + e.getMessage());
            return null;
        }
    }
}
