package com.na.common.feign;

import com.na.common.constant.IGlobalConst;
import com.na.common.exceptions.UnauthorizedException;
import com.na.common.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Feign 内部调用安全校验切面
 * <p>
 * 拦截 {@link Feign} 注解的方法, 校验请求头是否携带 Feign 标识:
 * <ul>
 *   <li>{@code from: Y} — 标识为内部调用</li>
 *   <li>{@code na-feign: Y} — Feign User-Agent 标识</li>
 * </ul>
 * 两个请求头都匹配时放行, 否则抛出 {@link UnauthorizedException}
 * <p>
 * 配置示例:
 * <pre>{@code
 * na:
 *   feign:
 *     enable: true   # 开启 Feign 安全校验(默认关闭)
 * }</pre>
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(
        name = "na.feign.enable",
        havingValue = "true",
        matchIfMissing = false
)
public class FeignAop {

    /**
     * 切点: 拦截标注了 {@link Feign} 注解的方法
     */
    @Pointcut("@annotation(com.na.common.feign.Feign)")
    public void pointCut() {
    }

    /**
     * 环绕通知: 校验 Feign 请求头标识
     *
     * @param pjp 连接点
     * @return 原方法返回值
     * @throws Throwable 原方法抛出的异常或 UnauthorizedException
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = CommonUtil.getCurrentHttpRequest();
        if (request != null) {
            String from = request.getHeader(IGlobalConst.FEIGN.FROM);
            String feign = request.getHeader(IGlobalConst.FEIGN.FEIGN_USER_AGENT);
            // 请求头 from=Y 且 na-feign=Y 时, 判定为合法 Feign 内部调用
            if (IGlobalConst.FEIGN.YES_NO.equals(from) && IGlobalConst.FEIGN.YES_NO.equals(feign)) {
                return pjp.proceed();
            }
        }
        // 非法请求: 缺少 Feign 标识请求头
        throw new UnauthorizedException("非法请求", null);
    }
}
