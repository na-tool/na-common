package com.na.common.aop;

import com.na.common.annotation.NaFeign;
import com.na.common.constant.INaGlobalConst;
import com.na.common.exceptions.NaUnauthorizedException;
import com.na.common.utils.NaCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(
        name = {"na.feign"},
        matchIfMissing = true
)
public class NaFeignAop {

    // 定义切点，匹配所有标注了@NaNoRepeatSubmit注解的方法
    @Pointcut("@annotation(com.na.common.annotation.NaFeign)")
    public void pointCut() {
    }

    // 环绕通知，拦截匹配切点的方法
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 1. 获取方法签名
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // 2. 判断方法上是否有 @NoToken 注解
        if (!method.isAnnotationPresent(NaFeign.class)) {
            return pjp.proceed();
        }

        // 3. 也可以获取类级注解
//        Class<?> clazz = method.getDeclaringClass();
//        if (clazz.isAnnotationPresent(NaFeign.class)) {
//            return pjp.proceed();
//        }

        // 4. 原有逻辑：通过请求头判断
        HttpServletRequest request = NaCommonUtil.getCurrentHttpRequest();
        if (request != null) {
            String from = request.getHeader(INaGlobalConst.SECURITY.FROM);
            String feign = request.getHeader(INaGlobalConst.SECURITY.FEIGN_USER_AGENT);
            if (INaGlobalConst.SECURITY.YES_NO.equals(from) && INaGlobalConst.SECURITY.YES_NO.equals(feign)) {
                return pjp.proceed();
            }
        }

        throw new NaUnauthorizedException("非法请求", null);
    }
}
