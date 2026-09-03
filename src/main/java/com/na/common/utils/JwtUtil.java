package com.na.common.utils;

import com.alibaba.fastjson.JSONObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（基于 jjwt 0.11.x）
 * <p>
 * 提供两组 API，签名密钥派生方式不同，生成与解析必须成对使用，不可混用：
 * <ul>
 *   <li>现代 API: {@link #generateToken(String, String, long)} / {@link #parseToken(String, String)}
 *       — 密钥使用 UTF-8 原始字节，HS256 要求 secret 长度 >= 32 字节，否则抛 WeakKeyException</li>
 *   <li>旧版 API: {@link #generateToken(String, String)} / {@link #generateTokenOld(String, String, long)} /
 *       {@link #parseTokenOld(String, String)} / {@link #parseJwtPayloadInformation(String, String, Class)}
 *       — 密钥字符串按 Base64 解码（jjwt 对 String 密钥的默认行为），用于兼容存量 token</li>
 * </ul>
 * 安全提示: secret 属于敏感信息，应从配置中心/环境变量注入，请勿硬编码或写入代码仓库
 */
@Slf4j
public final class JwtUtil {

    /** HS256 要求的最小密钥长度(字节)，不足时 hmacShaKeyFor 会抛 WeakKeyException */
    private static final int HS256_MIN_SECRET_BYTES = 32;

    private JwtUtil() {
    }

    /**
     * 生成 JWT Token（现代 API，推荐使用）
     * <p>
     * 使用 HMAC-SHA256 签名，密钥为 secret 的 UTF-8 字节，要求长度 >= 32 字节
     *
     * @param subject   载荷主体(通常存放用户标识或 JSON 业务数据)
     * @param secret    签名密钥，长度必须 >= 32 字节
     * @param ttlMillis 有效期(毫秒)，从当前时刻起算
     * @return JWT Token 字符串
     */
    public static String generateToken(String subject, String secret, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + ttlMillis))
                .signWith(createHmacKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT Token 并返回 subject（现代 API，推荐使用，与 {@link #generateToken(String, String, long)} 配对）
     * <p>
     * 校验签名与有效期，任一校验失败均返回 null
     *
     * @param token  JWT Token
     * @param secret 签名密钥，与生成时一致
     * @return subject；token 无效、过期、密钥不匹配或入参为 null 时返回 null
     */
    public static String parseToken(String token, String secret) {
        if (token == null || secret == null) {
            return null;
        }

        try {
            Jws<Claims> jwsClaims = Jwts.parserBuilder()
                    .setSigningKey(createHmacKey(secret))
                    .build()
                    .parseClaimsJws(token);

            // 签名与有效期校验通过，返回 subject
            return jwsClaims.getBody().getSubject();
        } catch (JwtException e) {
            // 包含签名无效(SignatureException)、过期(ExpiredJwtException)、格式错误(MalformedJwtException)等
            log.warn("Invalid or expired JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 创建 HMAC-SHA256 密钥
     *
     * @param secret 密钥字符串
     * @return HMAC 密钥对象
     */
    private static SecretKey createHmacKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成无有效期的 JWT Token（旧版 API，密钥按 Base64 解码）
     *
     * @param subject 载荷主体
     * @param salt    签名密钥字符串
     * @return JWT Token 字符串
     * @deprecated 密钥按 Base64 派生且永不过期，存在安全隐患；请使用
     * {@link #generateToken(String, String, long)}，并保证密钥长度 >= {@value #HS256_MIN_SECRET_BYTES} 字节
     */
    @Deprecated
    public static String generateToken(String subject, String salt) {
        // signWith(算法, String) 为废弃 API: salt 按 Base64 解码作为密钥，与 parseTokenOld 系列配对使用
        return Jwts.builder()
                .setSubject(subject)
                .signWith(SignatureAlgorithm.HS512, salt)
                .compact();
    }

    /**
     * 生成带有效期的 JWT Token（旧版 API，密钥按 Base64 解码）
     *
     * @param subject   载荷主体
     * @param salt      签名密钥字符串
     * @param ttlMillis 有效期(毫秒)
     * @return JWT Token 字符串
     * @deprecated 密钥按 Base64 派生；请使用 {@link #generateToken(String, String, long)}
     */
    @Deprecated
    public static String generateTokenOld(String subject, String salt, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        // signWith(算法, String) 为废弃 API: salt 按 Base64 解码作为密钥，与 parseTokenOld 系列配对使用
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(nowMillis))
                .signWith(SignatureAlgorithm.HS256, salt)
                .setExpiration(new Date(nowMillis + ttlMillis))
                .compact();
    }

    /**
     * 解析 JWT Token，将 subject 反序列化为指定类型对象（旧版 API，密钥按 Base64 解码）
     * <p>
     * 与 {@link #generateToken(String, String)} 或 {@link #generateTokenOld(String, String, long)} 配对使用，
     * 要求 subject 内容为 JSON 字符串
     *
     * @param token  被解析的 token
     * @param secret 签名密钥，与生成时一致
     * @param type   目标反序列化类型
     * @param <T>    泛型
     * @return 附属内容对象；token 无效或反序列化失败时返回 null
     * @deprecated 密钥按 Base64 派生；请使用 {@link #parseToken(String, String)} 后自行反序列化
     */
    @Deprecated
    public static <T> T parseJwtPayloadInformation(String token, String secret, Class<T> type) {
        try {
            // parser().setSigningKey(String) 为废弃 API: secret 按 Base64 解码，需与旧版生成 API 配对
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            String subject = claims.getSubject();
            return JSONObject.parseObject(subject, type);
        } catch (Exception e) {
            // 统一兜底: 签名/过期/格式/JSON 反序列化异常均返回 null
            log.error("JWT token parse exception", e);
            return null;
        }
    }

    /**
     * 解析 JWT Token 并返回 subject（旧版 API，密钥按 Base64 解码）
     * <p>
     * 与 {@link #generateToken(String, String)} 或 {@link #generateTokenOld(String, String, long)} 配对使用
     *
     * @param token  被解析的 token
     * @param secret 签名密钥，与生成时一致
     * @return subject；token 无效或入参为 null 时返回 null
     * @deprecated 密钥按 Base64 派生；请使用 {@link #parseToken(String, String)}
     */
    @Deprecated
    public static String parseTokenOld(String token, String secret) {
        try {
            // parser().setSigningKey(String) 为废弃 API: secret 按 Base64 解码，需与旧版生成 API 配对
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("JWT token parse exception", e);
            return null;
        }
    }
}
