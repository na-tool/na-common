package com.na.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.function.Function;

/**
 * 访问地址工具类：用于获取客户端真实 IP 地址（支持代理场景、多级代理、本地回环）
 * <p>
 * 同时支持 Servlet(WebMVC) 与 Reactive(WebFlux/Gateway) 两种请求模型，
 * 所有方法均为静态调用。注意：代理头(如 X-Forwarded-For)可被客户端伪造，
 * 依赖其做安全控制时，需确保请求经过可信代理并覆盖了伪造字段。
 */
@Slf4j
public class AddressUtil {

    /** 常见的代理转发请求头，按优先级排列，用于提取真实客户端 IP */
    private static final String[] IP_HEADER_NAMES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * 获取客户端真实 IP 地址（支持代理转发、多级代理、IPv6 回环、本地回环处理）
     *
     * @param request 可选，若为 null 则尝试从 Spring 上下文中自动获取当前请求
     * @return 客户端 IP 地址，若无法识别返回空字符串
     */
    public static String getIpAddress(HttpServletRequest request) {
        // 未显式传入 request 时，从 RequestContextHolder 上下文中获取
        if (request == null) {
            request = CommonUtil.getCurrentHttpRequest();
        }
        if (request == null) {
            return "";
        }

        try {
            // 依次从代理头提取，取不到再回退到 Servlet 的远程地址
            String ip = pickIpFromHeaders(request::getHeader);
            if (ip == null) {
                ip = request.getRemoteAddr();
            }
            return normalizeIp(ip);
        } catch (Exception e) {
            log.warn("获取客户端 IP 异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * WebFlux / Gateway 环境获取客户端真实 IP
     *
     * @param request Reactive 请求对象
     * @return 客户端 IP 地址，若无法识别返回空字符串
     */
    public static String getIpAddressServer(ServerHttpRequest request) {
        if (request == null) {
            return "";
        }

        try {
            String ip = pickIpFromHeaders(header -> request.getHeaders().getFirst(header));
            if (ip == null) {
                InetSocketAddress remoteAddress = request.getRemoteAddress();
                // 远程地址缺失时直接返回空，避免 NPE 控制流
                if (remoteAddress == null) {
                    return "";
                }
                ip = remoteAddress.getAddress().getHostAddress();
            }
            return normalizeIp(ip);
        } catch (Exception e) {
            log.warn("获取客户端 IP 异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 按优先级遍历代理头，返回第一个有效(非空且非unknown)的头值
     *
     * @param headerGetter 按头名称取值的函数
     * @return 有效头值；全部无效时返回 null
     */
    private static String pickIpFromHeaders(Function<String, String> headerGetter) {
        for (String header : IP_HEADER_NAMES) {
            String headerValue = headerGetter.apply(header);
            if (headerValue != null && !headerValue.isEmpty() && !"unknown".equalsIgnoreCase(headerValue)) {
                return headerValue;
            }
        }
        return null;
    }

    /**
     * 统一规范化 IP：
     * 1. 多级代理时 X-Forwarded-For 形如 "clientIp, proxy1, proxy2"，取第一个即真实客户端 IP
     * 2. IPv6 回环地址(::1)统一映射为 127.0.0.1
     * 3. 本机回环访问时，尝试解析为本机局域网 IP，便于日志与内部通信场景识别
     *
     * @param ip 原始 IP
     * @return 规范化后的 IP；入参无效时返回 null
     */
    private static String normalizeIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        // 多级代理取第一个 IP
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // IPv6 回环地址映射为 IPv4 回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        // 回环地址说明是本机访问，尝试取本机实际局域网 IP
        if ("127.0.0.1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.warn("无法解析本机 IP，使用默认回环地址 127.0.0.1");
            }
        }
        return ip;
    }

    /**
     * 获取本机 MAC 地址，格式如 "A1-B2-C3-D4-E5-F6"
     *
     * @return MAC 地址字符串；获取失败返回 null
     */
    public static String getMacAddress() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(ip);

            if (ni == null) {
                // 本机 IP 未绑定到具体网卡(常见于多网卡/虚拟化环境)，遍历网卡取第一个非回环且有硬件地址的
                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface element = nis.nextElement();
                    if (!element.isLoopback() && element.getHardwareAddress() != null) {
                        ni = element;
                        break;
                    }
                }
            }

            if (ni == null) {
                log.warn("未找到可用的网络接口，无法获取 MAC 地址");
                return null;
            }

            byte[] mac = ni.getHardwareAddress();
            if (mac == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (byte b : mac) {
                sb.append(String.format("%02X-", b));
            }
            // 去掉末尾多余的 '-'
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取 MAC 地址失败: {}", e.getMessage(), e);
            return null;
        }
    }

}
