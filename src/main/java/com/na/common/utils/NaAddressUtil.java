package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * 自定义访问地址工具类
 * 获取请求的ip地址等信息
 */
@Component
@Slf4j
public class NaAddressUtil {
    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr();的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？
     * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
     *
     * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130,
     * 192.168.1.100
     *
     * 用户真实IP为： 192.168.1.110
     * @param request request
     * @return ip
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        try {
            String ip = null;
            for (String header : new String[]{
                    "X-Forwarded-For",
                    "Proxy-Client-IP",
                    "WL-Proxy-Client-IP",
                    "HTTP_X_FORWARDED_FOR",
                    "HTTP_CLIENT_IP",
                    "HTTP_FORWARDED_FOR",
                    "HTTP_FORWARDED",
                    "HTTP_VIA",
                    "REMOTE_ADDR"
            }) {
                String value = request.getHeader(header);
                if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                    ip = value;
                    break;
                }
            }

            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // 多级代理时取第一个 IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            // IPv6 本地地址转 IPv4
            if ("0:0:0:0:0:0:0:1".equals(ip)) {
                ip = "127.0.0.1";
            }

            // 进一步处理本地 IP 情况
            if ("127.0.0.1".equals(ip)) {
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (UnknownHostException ignored) {
                }
            }

            return ip;
        } catch (Exception e) {
            log.warn("获取客户端IP异常: {}", e.getMessage());
            return "";
        }
    }
}
