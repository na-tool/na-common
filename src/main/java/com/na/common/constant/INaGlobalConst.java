package com.na.common.constant;

import org.springframework.http.HttpHeaders;

public interface INaGlobalConst {

    // 安全配置
    interface SECURITY{
        String JWT_TOKEN_PREFIX = "Bearer";
        String HEADER_STRING = HttpHeaders.AUTHORIZATION;

        String TOKEN_PREFIX = "Token";
        String TOKEN_API_PREFIX = "AuthToken";

        String REQUEST_SOURCE = "Request-Source";

        // APP
        String APP = "APP";
        // 浏览器
        String BROWSER = "BROWSER";
        // 小程序
        String MINI = "MINI";
        // 后台管理
        String MANAGER = "MANAGER";

        String[] SKIP_URLS = new String[]{
                "/v2/api-docs",
                "/v2/*",
                "/*/api-docs",
                "/druid/**",
                "/swagger-ui.html",
                "/doc.html",
                "/swagger-resources/**",
                "/webjars/**",
                "/csrf",
                "/favicon.ico",
                "/static/**",
                "/web/**",
                "/index/**",
        };
        String[] ACTUATOR_URLS = new String[]{"/actuator", "/", "/actuator/**" };

        Long EXPIRATION_MILLISECONDS = 1000L * 60 * 60 * 24 * 7;

        String STRING_KEY = "8a7f26bc-108c-41cb-8adb-1d5ed7b9a998";

        interface SYS_TYPE {
            String SYS_DEF = "00"; // 系统默认
            String SYS_ADMIN = "01"; // 系统管理员
            String DEF = "02"; // 默认
            String ORDINARY = "03"; // 普通
        }

        // 1默认启用 0关闭
        interface STATUS{
            Integer ENABLE = 1;
            Integer DISABLE = 0;
        }

        interface SYS_MENU_DATA_SCOPE {
            String ALL = "1";
            String CUSTOM = "2";
            String DEPT = "3";
            String DEPT_CHILD = "3";
        }

        interface SYS_MENU_PARENT {
            Long PARENT_ID = 0L;
        }

        //用户来源  0授权系统 1外部系统
        interface USER_FROM{
            Integer SYS = 0;
            Integer OUT = 1;
        }

        static Boolean isSysDef(String type) {
            // 系统默认类型
            return SYS_TYPE.SYS_DEF.equals(type);
        }
    }

    // 应用配置
    interface TOOLS{
        String CT = "CT"; // 合同
        String AUTH = "AUTH"; // 权限
    }

    interface CACHE{
        String REPEATED_SUBMIT_KEY = "REPEATED:SUMBIT:"; //
    }


}
