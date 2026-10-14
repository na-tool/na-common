package com.na.common.constant;

import org.springframework.http.HttpHeaders;

public interface IGlobalConst {

    // 安全配置
    interface SECURITY{
        String JWT_TOKEN_PREFIX = "Bearer";
        String HEADER_STRING = HttpHeaders.AUTHORIZATION;

        String REQUEST_SOURCE = "Request-Source";

        interface APP{
            // APP
            String APP = "APP";
            // 安卓
            String ANDROID = "ANDROID";
            // IOS
            String IOS = "IOS";
            // 浏览器
            String BROWSER = "BROWSER";
            // h5
            String H5 = "H5";
            // PC
            String PC = "PC";
            // 小程序
            String MINI = "MINI";

        }


        String[] SKIP_URLS = new String[]{
                // SpringDoc OpenAPI3（重点新增）
                "/v3/api-docs",
                "/v3/api-docs/**",
                // knife4j增强doc.html
                "/doc.html",
                "/webjars/**",
                "/swagger-ui/**",
                "/swagger-ui.html",

                // 旧swagger2兼容（保留也没关系，现在不用）
                "/v2/api-docs",
                "/v2/*",
                "/*/api-docs",
                "/swagger-resources/**",

                "/druid/**",
                "/csrf",
                "/favicon.ico",
                "/static/**",
                "/web/**",
                "/index/**"
        };
        String[] ACTUATOR_URLS = new String[]{"/actuator", "/", "/actuator/**" };

        Long EXPIRATION_MILLISECONDS = 1000L * 60 * 60 * 24 * 7;

        /**
         * 历史遗留的"默认密钥"。
         *
         * <p><b>它不是一个密钥</b>：本常量随公开库一起发布，任何人都能读到，
         * 因此任何依赖它的签名/加密都不具备机密性 —— 持有该值的攻击者可以伪造令牌或解开密文。
         *
         * <p>当前 {@code na-auth} 的 {@code AutoAuthConfig#getSigningKey()} 在宿主未配置
         * {@code na.auth.signingKey} 时回退到本常量，即把 JWT 签名密钥变成了公开已知值。
         * 生产部署必须显式配置自己的密钥（从配置中心或环境变量注入）。
         *
         * @deprecated 仅为兼容既有调用保留，不要在新代码中使用；请改为从配置读取。
         */
        @Deprecated
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

            /** 正常（对应数据库 0） */
            Boolean NORMAL = Boolean.FALSE;
            /** 停用（对应数据库 1） */
            Boolean DISABLED = Boolean.TRUE;
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

    interface FEIGN{
        /**
         * 是否内部调用
         */
        String YES_NO = "Y";

        /**
         * 标记来源
         */
        String FROM = "from";

        /**
         * Feign 代理调用
         */
        String FEIGN_USER_AGENT = "na-feign";
    }

    interface CACHE{
        String REPEATED_SUBMIT_KEY = "REPEATED:SUMBIT:"; //
    }


}
