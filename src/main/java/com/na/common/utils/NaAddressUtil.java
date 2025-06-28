package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 访问地址工具类：用于获取客户端真实 IP 地址（支持代理场景、本地回环）
 * 可直接用于 Spring Web 环境的静态调用
 */
@Component
@Slf4j
public class NaAddressUtil {

    // 常见的请求头，用于代理服务器转发的真实客户端 IP 提取
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
     * 获取客户端真实 IP 地址（支持代理转发、多级 IP、IPv6、本地回环处理）
     *
     * @param request 可选，若为 null 则尝试从 Spring 上下文中自动获取当前请求
     * @return 客户端 IP 地址，若无法识别返回空字符串
     */
    public static String getIpAddress(HttpServletRequest request) {
        // 若未显式传入 request，则从上下文中尝试获取
        if (request == null) {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes) {
                request = ((ServletRequestAttributes) attrs).getRequest();
            }
        }

        if (request == null) {
            return "";
        }

        try {
            String ip = null;

            // 遍历常见代理头部，优先获取非 unknown 且非空的 IP
            for (String header : IP_HEADER_NAMES) {
                String headerValue = request.getHeader(header);
                if (headerValue != null && !headerValue.isEmpty() && !"unknown".equalsIgnoreCase(headerValue)) {
                    ip = headerValue;
                    break;
                }
            }

            // 若 header 中都未获取到，则使用 Servlet 提供的远程地址
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // 多级代理中 X-Forwarded-For 可能返回多个 IP，用逗号分隔，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            // 将 IPv6 的回环地址 ::1 映射为 IPv4 127.0.0.1
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }

            // 若获取到的 IP 为本地回环地址，尝试获取实际的本机局域网 IP
            if ("127.0.0.1".equals(ip)) {
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (UnknownHostException ignored) {
                    log.warn("无法解析本机 IP，返回默认 127.0.0.1");
                }
            }

            return ip;
        } catch (Exception e) {
            log.warn("获取客户端 IP 异常: {}", e.getMessage());
            return "";
        }
    }
}
