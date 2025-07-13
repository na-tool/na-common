package com.na.common.utils;

import com.na.common.constant.NaConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

}
