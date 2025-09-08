package com.na.common.utils;

import com.alibaba.fastjson.JSONObject;
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

    public static String generateToken(String subject, String salt) {
        return Jwts.builder()
                .setSubject(subject)
                .signWith(SignatureAlgorithm.HS512, salt)
                .compact();
    }

    public static String generateTokenOld(String subject, String salt, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();//生成JWT的时间
        Date now = new Date(nowMillis);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .signWith(SignatureAlgorithm.HS256, salt)
                .setExpiration(new Date(nowMillis+ttlMillis))
                .compact();
    }

    /**
     * 用于JWT token 分析 附属内容
     * @param token 被分析的token
     * @param secret 签名Key
     * @param type 串行类型
     * @param <T> 泛型
     * @return 返回附属内容对象
     */
    public static <T> T parseJwtPayloadInformation(String token, String secret, Class<T> type) {
        T model = null;
        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            String subject = claims.getSubject();
            model = JSONObject.parseObject(subject, type);
        }
        catch (Exception e) {
            log.error("JWT token parse exception", e);
            model = null;
        }

        return model;
    }

    /**
     * 用于JWT token 分析 附属内容
     *
     * @param token  被分析的token
     * @param secret 签名Key
     * @return 返回附属内容对象
     */
    public static Claims parseTokenOld(String token, String secret) {
        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            String subject = claims.getSubject();
        } catch (Exception e) {
            log.error("JWT token parse exception", e);
            claims = null;
        }
        return claims;
    }
}
