package com.na.common.feign;

import com.na.common.constant.IGlobalConst;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * Feign 内部调用请求拦截器
 * <p>
 * 为每个 Feign 请求自动注入内部调用标识请求头:
 * <ul>
 *   <li>{@code na-feign: Y} — 所有 Feign 请求都携带, 标识为内部调用</li>
 *   <li>{@code from: Y} — 仅 {@link Token} 注解的方法携带, 标识无需 Token</li>
 * </ul>
 * <br>配合 {@link FeignAop} 使用, FeignAop 校验这两个请求头判断是否为合法内部调用
 * <p>
 * 执行顺序: {@link Ordered#HIGHEST_PRECEDENCE}, 确保在其他拦截器之前注入请求头
 */
@Slf4j
public class FeignInnerRequestInterceptor implements RequestInterceptor, Ordered {

    /**
     * 为 Feign 请求注入内部调用标识请求头
     *
     * @param template Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        Method method = template.methodMetadata().method();
        Token noToken = method.getAnnotation(Token.class);
        if (noToken != null) {
            // @Token 方法: 注入 from=Y 标识无需 Token
            template.header(IGlobalConst.FEIGN.FROM, IGlobalConst.FEIGN.YES_NO);
        }

        // 所有 Feign 请求: 注入 na-feign=Y 内部调用标识
        template.header(IGlobalConst.FEIGN.FEIGN_USER_AGENT, IGlobalConst.FEIGN.YES_NO);
        log.info("Feign interceptor hit: {}", template.url());
    }

    /**
     * 拦截器执行顺序(最高优先级, 确保请求头最先注入)
     *
     * @return {@link Integer#MIN_VALUE}
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
