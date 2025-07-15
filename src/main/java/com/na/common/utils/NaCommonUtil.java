package com.na.common.utils;

import com.na.common.constant.NaConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class NaCommonUtil {
    /**
     * 是否为http(s)://开头
     *
     * @param link 链接
     * @return 结果
     */
    public static boolean ishttp(String link)
    {
        return StringUtils.startsWithAny(link, NaConst.HTTP, NaConst.HTTPS);
    }

    /**
     * 判断是否为windows操作系统
     * @return true 是，false 否
     */
    public static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win");
    }

    /**
     * 对字符串进行脱敏处理，保留前3位和后2位，中间部分用星号替代。
     * 示例：
     * {@code mask("张三丰12345") ->} 张三***45
     * {@code mask("abcde") ->} abc***de
     * {@code mask("abc") ->} abc***（长度不足时自动补***）
     *
     * @param input 输入字符串
     * @return 脱敏后的字符串
     */
    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;  // 空或 null 不处理
        }

        int len = input.length();

        // 长度 <= 5，返回原始 + ***
        if (len <= 5) {
            return input + "***";
        }

        // 正常脱敏逻辑：保留前3位和后2位
        String prefix = input.substring(0, 3);
        String suffix = input.substring(len - 2);
        return prefix + "***" + suffix;
    }

    /**
     * 支持简单通配符和正则匹配的路径判断
     * 判断路径是否在 xss 路径白名单中
     * @param requestUris 白名单路径列表，可以包含通配符，如 "/api/*"
     * @param requestUri 当前请求的路径
     * @return 如果请求路径匹配白名单中的任一路径，返回 true；否则返回 false
     */
    public static boolean isExcluded(List<String> requestUris, String requestUri) {
        for (String uri : requestUris) {
            // 将 uriPattern 转换为正则表达式
            String regex = uri.replace("/*", "/[^/]*");

            // 使用 Pattern 进行匹配
            if (Pattern.matches(regex, requestUri)) {
                log.debug("[XSS] 命中路径白名单: {}", requestUri);
                return true;
            }
        }
        return false;
    }

    public static HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;
    }

    public static HttpServletResponse getCurrentHttpResponse() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getResponse();
        }
        return null;
    }

    /**
     * 判断主机（域名或IP）是否匹配白名单规则。
     * 支持的匹配方式包括：
     * <ul>
     *   <li>精确匹配，如 "www.example.com" 或 "192.168.1.10"</li>
     *   <li>IP段通配符匹配，如 "192.168.1.*" 可匹配 "192.168.1.8"</li>
     *   <li>IP范围匹配，如 "192.168.1.10-192.168.1.20"</li>
     *   <li>纯域名匹配（不支持通配符），需精确匹配</li>
     * </ul>
     *
     * @param patternHost 白名单中配置的主机模式，可能是IP、IP段通配、IP范围或域名
     * @param requestHost 当前请求的主机，包含域名或IP（不含端口）
     * @return 如果 requestHost 符合 patternHost 的匹配规则，则返回 true；否则返回 false
     */
    public static boolean isHostMatch(String patternHost, String requestHost) {
        if (patternHost.equalsIgnoreCase(requestHost)) {
            return true; // 精确匹配
        }

        // 判断是否是IP格式（4段数字）
        if (isIp(patternHost) && isIp(requestHost)) {
            return isIpSegmentMatch(patternHost, requestHost);
        }

        // 不支持通配的域名，必须精确匹配，已判断过精确匹配，故这里返回false
        return false;
    }

    /**
     * 判断给定的字符串是否为合法的IPv4地址格式。
     * 简单校验是否为四段数字组成，格式如 "192.168.1.1"。
     * 注意：此方法不校验每段数字是否在0~255范围内，只判断格式是否为四段数字。
     *
     * @param host 待校验的主机字符串，通常是IP地址
     * @return 如果符合简单IPv4格式，返回 true；否则返回 false
     */
    public static boolean isIp(String host) {
        return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }

    /**
     * 判断IP地址是否匹配IP段模式。
     * 支持的模式示例：
     * - 通配符 "*"：192.168.1.*
     * - 单段数字范围：192.168.1.10-20
     * - 整段IP范围：192.168.1.10-192.168.1.20 （本方法调用了 isIpRangeMatch 实现此功能）
     *
     * @param pattern IP段模式字符串
     * @param ip 待匹配的IP地址字符串
     * @return 如果IP匹配pattern规则，返回true，否则false
     */
    public static boolean isIpSegmentMatch(String pattern, String ip) {
        String[] patternParts = pattern.split("\\.");
        String[] ipParts = ip.split("\\.");

        if (patternParts.length != 4 || ipParts.length != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            String p = patternParts[i];
            String ipSegment = ipParts[i];

            if (p.equals("*")) {
                // 通配符，跳过该段匹配
                continue;
            }

            if (p.contains("-")) {
                // 支持两种范围写法：
                // 1）单段范围，如 10-20
                // 2）整段IP范围，如 192.168.1.10-192.168.1.20（其实本函数处理单段，不处理全IP范围）
                if (p.contains(".")) {
                    // 整段IP范围
                    if (!isIpRangeMatch(p, ip)) {
                        return false;
                    }
                    // 匹配成功，直接返回true（因为整段匹配了）
                    return true;
                } else {
                    // 单段范围匹配
                    String[] range = p.split("-");
                    int low = Integer.parseInt(range[0]);
                    int high = Integer.parseInt(range[1]);
                    int val = Integer.parseInt(ipSegment);
                    if (val < low || val > high) {
                        return false;
                    }
                }
            } else {
                // 精确匹配
                if (!p.equals(ipSegment)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 整段IP范围匹配，例如pattern：192.168.1.10-192.168.1.20
     * @param patternRange IP范围字符串，格式为 起始IP-结束IP
     * @param ip 目标IP地址
     * @return 是否在范围内
     */
    public static boolean isIpRangeMatch(String patternRange, String ip) {
        String[] ips = patternRange.split("-");
        if (ips.length != 2) {
            return false;
        }
        long start = ipToLong(ips[0]);
        long end = ipToLong(ips[1]);
        long target = ipToLong(ip);
        return target >= start && target <= end;
    }

    /**
     * 将点分十进制IP地址转换为long数字
     * 例如：192.168.1.10 -&gt; 3232235786L
     *
     * @param ip 点分十进制字符串形式的IP地址
     * @return IP对应的long数字表示
     */
    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long res = 0;
        for (int i = 0; i < 4; i++) {
            res <<= 8;
            res |= Integer.parseInt(parts[i]) & 0xFF;
        }
        return res;
    }

    /**
     * 端口匹配，支持单个端口或端口范围
     * 例如：
     * "8080"          -&gt; 精确匹配端口8080
     * "8000-9000"     -&gt; 匹配端口范围8000到9000（包含边界）
     *
     * @param patternPort 端口匹配模式，支持单个端口或端口范围
     * @param port 实际端口数字
     * @return 是否匹配成功
     */
    public static boolean isPortMatch(String patternPort, int port) {
        if (!patternPort.contains("-")) {
            // 精确端口匹配
            try {
                int p = Integer.parseInt(patternPort);
                return p == port;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            // 端口范围匹配
            String[] range = patternPort.split("-");
            if (range.length != 2) {
                return false;
            }
            try {
                int low = Integer.parseInt(range[0]);
                int high = Integer.parseInt(range[1]);
                return port >= low && port <= high;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }


}
