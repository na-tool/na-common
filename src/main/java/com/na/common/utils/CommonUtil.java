package com.na.common.utils;

import com.na.common.constant.GlobalConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 通用工具类
 * <p>
 * 提供链接判断、字符串脱敏、路径白名单匹配、主机/IP/端口白名单匹配、
 * 请求上下文获取等通用能力。匹配类方法对非法或 null 输入统一返回不匹配，不抛异常。
 */
@Slf4j
public class CommonUtil {

    /** 简单 IPv4 格式: 四段数字, 如 192.168.1.1 (不校验每段是否在 0~255 范围内) */
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");

    /** IP 段匹配模式: 每段可为数字、通配符(*)或单段范围(-), 如 192.168.1.* 、192.168.1.10-20 */
    private static final Pattern IP_SEG_PATTERN = Pattern.compile("[\\d*\\-]+\\.[\\d*\\-]+\\.[\\d*\\-]+\\.[\\d*\\-]+");

    /** 整段 IP 范围模式: 起始IP-结束IP, 如 192.168.1.10-192.168.1.20 */
    private static final Pattern IP_RANGE_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+-\\d+\\.\\d+\\.\\d+\\.\\d+");

    /** 纯静态工具类，禁止实例化 */
    private CommonUtil() {
    }

    /**
     * 是否为http(s)://开头
     *
     * @param link 链接
     * @return 结果
     */
    public static boolean ishttp(String link) {
        return StringUtils.startsWithAny(link, GlobalConst.HTTP, GlobalConst.HTTPS);
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
     * {@code mask("张三丰12345") ->} 张三丰***45
     * {@code mask("abcde") ->} abcde***（长度<=5时保留原文并追加***）
     * {@code mask("abc") ->} abc***
     *
     * @param input 输入字符串
     * @return 脱敏后的字符串
     */
    public static String mask(String input) {
        // 空或 null 不处理，原样返回
        if (input == null || input.isEmpty()) {
            return input;
        }

        int len = input.length();

        // 长度不足以保留前3后2时，降级为原文追加***
        if (len <= 5) {
            return input + "***";
        }

        // 正常脱敏逻辑：保留前3位和后2位
        return input.substring(0, 3) + "***" + input.substring(len - 2);
    }

    /**
     * 支持简单通配符匹配的路径白名单判断
     * 判断路径是否在 xss 等场景的路径白名单中
     * <p>
     * 通配符规则：{@code /*} 仅匹配单段路径，如 {@code /api/*} 可匹配 {@code /api/users} 但不匹配 {@code /api/users/1}
     *
     * @param requestUris 白名单路径列表，可以包含通配符，如 "/api/*"
     * @param requestUri  当前请求的路径
     * @return 如果请求路径匹配白名单中的任一路径，返回 true；否则返回 false
     */
    public static boolean isExcluded(List<String> requestUris, String requestUri) {
        // 空白名单或空请求路径直接视为不匹配
        if (requestUris == null || requestUris.isEmpty() || requestUri == null) {
            return false;
        }
        for (String uri : requestUris) {
            // 先转义正则特殊字符(如路径中的 . )，避免被当作正则元字符;
            // 再将通配符 /* 转为 /[^/]*，实现单段路径通配
            String regex = uri.replace("\\", "\\\\").replace(".", "\\.").replace("/*", "/[^/]*");

            if (Pattern.matches(regex, requestUri)) {
                log.debug("[XSS] 命中路径白名单: {}", requestUri);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前线程绑定的 HttpServletRequest
     * @return 当前请求对象；非Web上下文(如定时任务、MQ消费)中返回 null
     */
    public static HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        // instanceof 对 null 恒为 false，无需额外判空
        return attrs instanceof ServletRequestAttributes
                ? ((ServletRequestAttributes) attrs).getRequest() : null;
    }

    /**
     * 获取当前线程绑定的 HttpServletResponse
     * @return 当前响应对象；非Web上下文(如定时任务、MQ消费)中返回 null
     */
    public static HttpServletResponse getCurrentHttpResponse() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes
                ? ((ServletRequestAttributes) attrs).getResponse() : null;
    }

    /**
     * 判断主机（域名或IP）是否匹配白名单规则。
     * 支持的匹配方式包括：
     * <ul>
     *   <li>精确匹配，如 "www.example.com" 或 "192.168.1.10"</li>
     *   <li>IP段通配符匹配，如 "192.168.1.*" 可匹配 "192.168.1.8"</li>
     *   <li>IP单段范围匹配，如 "192.168.1.10-20"</li>
     *   <li>整段IP范围匹配，如 "192.168.1.10-192.168.1.20"</li>
     *   <li>纯域名匹配（不支持通配符），需精确匹配</li>
     * </ul>
     *
     * @param patternHost 白名单中配置的主机模式，可能是IP、IP段通配、IP范围或域名
     * @param requestHost 当前请求的主机，包含域名或IP（不含端口）
     * @return 如果 requestHost 符合 patternHost 的匹配规则，则返回 true；否则返回 false
     */
    public static boolean isHostMatch(String patternHost, String requestHost) {
        if (patternHost == null || requestHost == null) {
            return false;
        }

        // 1. 精确匹配(域名或IP)，域名忽略大小写
        if (patternHost.equalsIgnoreCase(requestHost)) {
            return true;
        }

        if (isIp(requestHost)) {
            // 2. 整段IP范围匹配，如 192.168.1.10-192.168.1.20
            //    注意: 该格式无法通过4段模式的校验，必须优先于IP段模式单独处理
            if (IP_RANGE_PATTERN.matcher(patternHost).matches()) {
                return isIpRangeMatch(patternHost, requestHost);
            }
            // 3. IP段模式匹配(通配符/单段范围)，如 192.168.1.* 、192.168.1.10-20
            if (isIpPattern(patternHost)) {
                return isIpSegmentMatch(patternHost, requestHost);
            }
        }

        // 域名不支持通配符，仅支持精确匹配
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
        return host != null && IPV4_PATTERN.matcher(host).matches();
    }

    /**
     * 判断是否为合法的 IP 段匹配模式（允许 * 或 - 存在于任意段中）。
     * <p>
     * 支持的格式：
     * <ul>
     *     <li>通配符：如 {@code 192.168.1.*}</li>
     *     <li>单段数字范围：如 {@code 192.168.1.10-20}</li>
     * </ul>
     * <p>
     * 注意：该方法只校验格式是否允许通配符（*）或范围（-）存在，并不校验 IP 的有效性或数值范围。
     * 整段IP范围（如 192.168.1.10-192.168.1.20）不满足此4段格式，由 isHostMatch 单独处理。
     *
     * @param host IP 模式字符串，例如 "192.168.1.*"、"192.168.1.10-20"
     * @return 如果是合法的 IP 段匹配模式，返回 true；否则返回 false
     */
    public static boolean isIpPattern(String host) {
        return host != null && IP_SEG_PATTERN.matcher(host).matches();
    }

    /**
     * 判断IP地址是否匹配IP段模式。
     * 支持的模式示例：
     * - 通配符 "*"：192.168.1.*
     * - 单段数字范围：192.168.1.10-20
     *
     * @param pattern IP段模式字符串
     * @param ip 待匹配的IP地址字符串
     * @return 如果IP匹配pattern规则，返回true，否则false
     */
    public static boolean isIpSegmentMatch(String pattern, String ip) {
        if (pattern == null || ip == null) {
            return false;
        }
        String[] patternParts = pattern.split("\\.");
        String[] ipParts = ip.split("\\.");

        if (patternParts.length != 4 || ipParts.length != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            String p = patternParts[i];
            String ipSegment = ipParts[i];

            if ("*".equals(p)) {
                // 通配符，该段任意值均匹配
                continue;
            }

            if (p.contains("-")) {
                // 单段范围匹配，如 10-20 (整段IP范围在 isHostMatch 中已先行处理)
                String[] range = p.split("-");
                if (range.length != 2) {
                    return false;
                }
                try {
                    int low = Integer.parseInt(range[0]);
                    int high = Integer.parseInt(range[1]);
                    int val = Integer.parseInt(ipSegment);
                    if (val < low || val > high) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // 模式串或IP段非法(如段中混合 *)，视为不匹配
                    return false;
                }
            } else if (!p.equals(ipSegment)) {
                // 精确匹配
                return false;
            }
        }

        return true;
    }

    /**
     * 整段IP范围匹配，例如pattern：192.168.1.10-192.168.1.20
     * @param patternRange IP范围字符串，格式为 起始IP-结束IP
     * @param ip 目标IP地址
     * @return 是否在范围内(含边界)
     */
    public static boolean isIpRangeMatch(String patternRange, String ip) {
        String[] ips = patternRange.split("-");
        // 校验范围两端及目标均为合法IPv4，防止非法格式导致解析异常
        if (ips.length != 2 || !isIp(ips[0]) || !isIp(ips[1]) || !isIp(ip)) {
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
     * @throws IllegalArgumentException IP不是四段数字格式时抛出
     */
    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("非法IPv4地址: " + ip);
        }
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
        if (patternPort == null) {
            return false;
        }
        String[] parts = patternPort.split("-");
        try {
            if (parts.length == 1) {
                // 精确端口匹配
                return Integer.parseInt(parts[0].trim()) == port;
            }
            if (parts.length == 2) {
                // 端口范围匹配(含边界)
                return port >= Integer.parseInt(parts[0].trim())
                        && port <= Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException e) {
            // 端口配置非法，视为不匹配
        }
        return false;
    }
}
