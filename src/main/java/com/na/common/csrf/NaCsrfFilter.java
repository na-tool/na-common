package com.na.common.csrf;

import com.alibaba.fastjson.JSONObject;
import com.na.common.exceptions.NaBusinessException;
import com.na.common.result.enums.NaStatus;
import com.na.common.utils.NaCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CSRF（跨站请求伪造）防护过滤器
 *
 * 原理：
 * 1. 通过 Referer 请求头判断请求来源是否为受信任域名；
 * 2. 如果来源不可信，判断当前请求路径是否在白名单中；
 * 3. 两者都不满足，则视为潜在的 CSRF 攻击，拦截请求。
 */
@Slf4j
public class NaCsrfFilter implements Filter {

    private final NaAutoCsrfConfig config;

    public NaCsrfFilter(NaAutoCsrfConfig config) {
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化方法（本过滤器无需初始化）
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 获取 Referer 请求头（用于判断请求来源）
        String referer = req.getHeader("Referer");

        // === 新增逻辑：允许 Referer 为空的配置判断 ===
        if (StringUtils.isBlank(referer)) {
            if (config.getAllowBlankReferer()) {
                // 放行空 Referer（如通过浏览器地址栏访问）
                chain.doFilter(request, response);
                return;
            } else {
                log.warn("[CSRF 拦截] Referer 为空，且未允许空 Referer 请求。");
//                res.sendRedirect(req.getContextPath() + "/illegal");
                throw new NaBusinessException(NaStatus.AUTHORIZATION_EXPIRED,null);
            }
        }

        // 提取 Referer 中的 host:port（如 www.example.com:8080）
        String refererHost = extractHostAndPort(req,res, referer);

        // 提取当前请求地址中的 host:port
        String requestHost = extractHostAndPort(req, res,null);

        // 如果 Referer 来源与当前请求来源不同，说明可能是跨站访问
        if (!requestHost.equalsIgnoreCase(refererHost)) {

            // 判断 Referer 来源是否在域名白名单中
            if (!isInDomainWhitelist(refererHost)) {

                // 获取请求的路径部分（不含域名）
                String requestPath = new URL(req.getRequestURL().toString()).getPath();

                // 去掉上下文路径，得到应用内真实访问路径
                String cleanPath = requestPath.replaceFirst(req.getContextPath(), "");

                /**
                 * 支持简单通配符和正则匹配的路径判断
                 * 判断路径是否在 CSRF 路径白名单中
                 */
                if (!NaCommonUtil.isExcluded(config.getCsrfWhitePaths(),cleanPath)) {
                    // 非白名单域名 且 非白名单路径，触发 CSRF 拦截
                    log.warn("[CSRF 拦截] Referer 不可信: {}", referer);
                    log.warn("[CSRF 拦截] 请求地址: {}", req.getRequestURL());
//                    res.sendRedirect(req.getContextPath() + "/illegal"); // 跳转到非法访问页面
                    throw new NaBusinessException(NaStatus.AUTHORIZATION_EXPIRED,null);
                }
            }
        }

        // 放行请求（Referer 合法 或 校验通过）
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁过滤器资源（本过滤器无需销毁资源）
    }

    /**
     * 提取 Host:Port 信息
     *
     * @param request  当前请求对象
     * @param response  当前响应对象
     * @param referer  如果非空，则使用 Referer，否则使用 request 自身 URL
     * @return host[:port] 字符串
     */
    private String extractHostAndPort(HttpServletRequest request,HttpServletResponse response, String referer) throws IOException {

        try {
            URL url = StringUtils.isNotBlank(referer)
                    ? new URL(referer)
                    : new URL(request.getRequestURL().toString());

            return (url.getPort() == -1) ? url.getHost() : url.getHost() + ":" + url.getPort();
        } catch (MalformedURLException e) {
            log.warn("无效 Referer：{}", referer);
//            response.sendRedirect(request.getContextPath() + "/illegal");
//            return "";
            throw new NaBusinessException(NaStatus.AUTHORIZATION_EXPIRED,null);
        }
    }

    /**
     * 判断域名是否在 CSRF 域名白名单中
     * 判断 refererHost 是否在白名单中
     * refererHost 可能是 ip[:port] 或 域名[:port]
     */
    private boolean isInDomainWhitelist(String refererHost) {
        log.debug("[CSRF] 命中域名白名单: {}", refererHost);
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

            // 解析白名单pattern中的端口（可能无端口）
            String[] patternSplit = pattern.split(":");
            String patternHost = patternSplit[0];
            String patternPortPart = patternSplit.length > 1 ? patternSplit[1] : null;

            // 先判断host部分是否匹配
            if (NaCommonUtil.isHostMatch(patternHost, host)) {
                // Host匹配，再判断端口
                if (patternPortPart == null) {
                    // 白名单没指定端口，任何端口都匹配
                    return true;
                } else {
                    // 白名单指定了端口或端口范围
                    if (port == null) {
                        // 请求没有端口，不匹配白名单指定端口
                        continue;
                    }
                    if (NaCommonUtil.isPortMatch(patternPortPart, port)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
