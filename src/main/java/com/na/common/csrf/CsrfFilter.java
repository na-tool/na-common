package com.na.common.csrf;

import com.na.common.exceptions.BusinessException;
import com.na.common.result.enums.Status;
import com.na.common.utils.CommonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * CSRF(跨站请求伪造)防护过滤器
 * <p>
 * 防护原理:
 * <ol>
 *   <li>通过 Referer 请求头判断请求来源是否为受信任域名</li>
 *   <li>若来源不可信, 判断当前请求路径是否在白名单中</li>
 *   <li>两者都不满足, 视为潜在 CSRF 攻击, 抛出 {@link BusinessException} 拦截</li>
 * </ol>
 * <p>
 * 由 {@link CsrfFilterConfig} 在 {@code na.csrf.enabled=true} 时注册
 */
@Slf4j
public class CsrfFilter implements Filter {

    private final AutoCsrfConfig config;

    /**
     * @param config CSRF 配置属性(白名单、空 Referer 策略等)
     */
    public CsrfFilter(AutoCsrfConfig config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 获取 Referer 请求头(用于判断请求来源)
        String referer = req.getHeader("Referer");

        // 允许 Referer 为空的配置判断
        if (StringUtils.isBlank(referer)) {
            if (Boolean.TRUE.equals(config.getAllowBlankReferer())) {
                // 放行空 Referer(如浏览器地址栏直接访问)
                chain.doFilter(request, response);
                return;
            } else {
                log.warn("[CSRF 拦截] Referer 为空, 且未允许空 Referer 请求");
                throw new BusinessException(Status.AUTHORIZATION_EXPIRED, null);
            }
        }

        // 提取 Referer 中的 host:port
        String refererHost = extractHostAndPort(req, referer);

        // 提取当前请求地址中的 host:port
        String requestHost = extractHostAndPort(req, null);

        // Referer 来源与当前请求来源不同, 可能是跨站访问
        if (!requestHost.equalsIgnoreCase(refererHost)) {

            // 判断 Referer 来源是否在域名白名单中
            if (!isInDomainWhitelist(refererHost)) {

                // 获取请求路径(不含域名)
                String requestPath = new URL(req.getRequestURL().toString()).getPath();

                // 去掉上下文路径, 得到应用内真实访问路径
                // 用 replace 而非 replaceFirst: contextPath 不是正则, 字面替换更安全
                String cleanPath = req.getContextPath().isEmpty()
                        ? requestPath
                        : requestPath.replace(req.getContextPath(), "");

                // 判断路径是否在 CSRF 路径白名单中
                if (!CommonUtil.isExcluded(config.getCsrfWhitePaths(), cleanPath)) {
                    log.warn("[CSRF 拦截] Referer 不可信: {}", referer);
                    log.warn("[CSRF 拦截] 请求地址: {}", req.getRequestURL());
                    throw new BusinessException(Status.AUTHORIZATION_EXPIRED, null);
                }
            }
        }

        // 放行请求(Referer 合法或校验通过)
        chain.doFilter(request, response);
    }

    /**
     * 提取 URL 中的 host[:port]
     *
     * @param request 当前请求对象
     * @param referer 如果非空, 从 Referer URL 提取; 为空则从 request 自身 URL 提取
     * @return host 或 host:port 字符串
     * @throws IOException URL 解析失败时抛出 BusinessException
     */
    private String extractHostAndPort(HttpServletRequest request, String referer) throws IOException {
        try {
            URL url = StringUtils.isNotBlank(referer)
                    ? new URL(referer)
                    : new URL(request.getRequestURL().toString());

            return (url.getPort() == -1) ? url.getHost() : url.getHost() + ":" + url.getPort();
        } catch (MalformedURLException e) {
            log.warn("[CSRF] 无效 Referer: {}", referer);
            throw new BusinessException(Status.AUTHORIZATION_EXPIRED, null);
        }
    }

    /**
     * 判断域名是否在 CSRF 域名白名单中
     * <p>refererHost 可能是 ip[:port] 或 域名[:port], 支持通配符与端口范围
     *
     * @param refererHost Referer 的 host[:port]
     * @return true 在白名单中, false 不在
     */
    private boolean isInDomainWhitelist(String refererHost) {
        log.debug("[CSRF] 检查域名白名单: {}", refererHost);
        List<String> whitelist = config.getCsrfWhiteDomains();
        if (CollectionUtils.isEmpty(whitelist) || StringUtils.isBlank(refererHost)) {
            return false;
        }

        // 拆分请求的 host 和端口
        String[] hostPortSplit = refererHost.split(":");
        String host = hostPortSplit[0];
        Integer port = null;
        if (hostPortSplit.length > 1) {
            try {
                port = Integer.parseInt(hostPortSplit[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        for (String pattern : whitelist) {
            if (StringUtils.isBlank(pattern)) {
                continue;
            }

            // 解析白名单 pattern 中的端口(可能无端口)
            String[] patternSplit = pattern.split(":");
            String patternHost = patternSplit[0];
            String patternPortPart = patternSplit.length > 1 ? patternSplit[1] : null;

            // 先判断 host 部分是否匹配
            if (CommonUtil.isHostMatch(patternHost, host)) {
                // Host 匹配, 再判断端口
                if (patternPortPart == null) {
                    // 白名单没指定端口, 任何端口都匹配
                    return true;
                } else {
                    // 白名单指定了端口或端口范围
                    if (port == null) {
                        // 请求没有端口, 不匹配白名单指定端口
                        continue;
                    }
                    if (CommonUtil.isPortMatch(patternPortPart, port)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
