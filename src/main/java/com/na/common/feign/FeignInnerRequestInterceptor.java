package com.na.common.feign;

import com.na.common.constant.IGlobalConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class NaFeignInnerRequestInterceptor implements RequestInterceptor, Ordered {

    @Override
    public void apply(RequestTemplate template) {
        Method method = template.methodMetadata().method();
        Token noToken = method.getAnnotation(Token.class);
        if (noToken != null) {
            // 来源标识
            template.header(IGlobalConst.FEIGN.FROM, IGlobalConst.FEIGN.YES_NO);
        }

        // Feign 代理内部调用标识
        template.header(IGlobalConst.FEIGN.FEIGN_USER_AGENT, IGlobalConst.FEIGN.YES_NO);
        log.info("Feign interceptor hit: {}", template.url());
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
