package com.na.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(
        name = {"na.corsFilter"},
        matchIfMissing = false
)
public class NaCorsFilter implements Filter {
    @Autowired
    private Environment environment;

    @Override
    public void doFilter(final ServletRequest req,
                         final ServletResponse res,
                         final FilterChain chain)
            throws IOException, ServletException {
        boolean isCors = Boolean.parseBoolean(environment.getProperty("na.cors.enabled", "true"));
        if (isCors) {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setHeader("Access-Control-Allow-Origin", "*"); // 注意，这里 * 和 Allow-Credentials=true 会冲突
            response.setHeader("Access-Control-Allow-Methods",
                    "POST, GET, OPTIONS, DELETE, PUT");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers",
                    "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, Access-Token, Authorization");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        chain.doFilter(req, res);

    }

    @Override
    public void init(final FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
