package com.na.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "na.corsFilter", matchIfMissing = false)
public class NaCorsFilter implements Filter {

    @Autowired
    private Environment environment;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        boolean isCors = Boolean.parseBoolean(environment.getProperty("na.cors.enabled", "true"));
        if (isCors) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isEmpty()) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
            }

            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");

            // ✅ 允许所有请求头或动态返回前端请求头
            String requestHeaders = request.getHeader("Access-Control-Request-Headers");
            if (requestHeaders != null) {
                response.setHeader("Access-Control-Allow-Headers", requestHeaders);
            } else {
                response.setHeader("Access-Control-Allow-Headers", "*");
            }

            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}

