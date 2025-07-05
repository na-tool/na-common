package com.na.common.aop;

import com.na.common.annotation.NaNoRepeatSubmit;
import com.na.common.annotation.NaNoRepeatSubmitIP;
import com.na.common.cache.NaCacheTemplate;
import com.na.common.constant.INaGlobalConst;
import com.na.common.exceptions.NaBusinessException;
import com.na.common.result.enums.NaStatus;
import com.na.common.utils.NaAddressUtil;
import com.na.common.utils.NaDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(
        name = {"na.submit"},
        matchIfMissing = false
)
public class NaNoRepeatSubmitAOP {
    @Autowired
    private Environment environment;
    @Autowired
    private INaNoRepeatSubmitUserService naNoRepeatSubmitUserService;

    // 定义切点，匹配所有标注了@NaNoRepeatSubmit注解的方法
    @Pointcut("@annotation(com.na.common.annotation.NaNoRepeatSubmit)")
    public void pointCut() {
    }

    // 环绕通知，拦截匹配切点的方法
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 获取当前HTTP请求
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(
                RequestContextHolder.getRequestAttributes())).getRequest();

        // 获取请求的IP地址
        String ip = NaAddressUtil.getIpAddress(request);
        // 获取请求头中的Authentication信息
        String authentication = request.getHeader(INaGlobalConst.SECURITY.HEADER_STRING);
        authentication = StringUtils.isEmpty(authentication) ? "auth" : authentication;

        String userId = naNoRepeatSubmitUserService.userId();

        // 构建唯一请求的Key
        String key = userId + authentication + "-" + request.getServletPath();

        // 获取注解上的过期时间，如果没有设置则使用默认值
        NaNoRepeatSubmit annotation = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(NaNoRepeatSubmit.class);
        long expire = annotation.value();
        int num = annotation.num();
        boolean showRemainingTime = annotation.showRemainingTime();
        boolean requestParams = annotation.requestParams();
        NaDateTimeUtil.DateFormat dateFormat = annotation.dateFormat();
//        String zoneId = annotation.zoneId();

        if(requestParams){
            // 构建请求参数字符串
            Map<String, String[]> parameterMap = request.getParameterMap();
            StringBuilder stringBuilder = new StringBuilder();
            parameterMap.forEach((k, v) -> stringBuilder.append(k).append(Arrays.toString(v)));

            // 构建唯一请求的Key
            key = userId + authentication + "-" + request.getServletPath() + "-" + stringBuilder;
        }

        key = INaGlobalConst.CACHE.REPEATED_SUBMIT_KEY + key;

        int cacheNum = 0;
        // 检查缓存中是否存在该请求
        Integer cache = NaCacheTemplate.getCache(key, Integer.class);
        if(cache != null){
            cacheNum = cache;
            Long remainingExpireTime = NaCacheTemplate.getRemainingExpireTime(key);
            if (cacheNum >= num) {
                log.warn("重复请求: {}", key);
                String msg = annotation.msg();
                if (showRemainingTime) {
                    if (remainingExpireTime != null && remainingExpireTime > 0) {
                        msg = msg + " 当前剩余时间：" + NaDateTimeUtil.parseToString(remainingExpireTime, dateFormat);
                    }
                }
                throw new NaBusinessException(StringUtils.isEmpty(msg) ? NaStatus.FAIL_REQUEST_REPETITION.getMsg() : msg, null);
            }
            // 剩余时间
            expire = (remainingExpireTime != null && remainingExpireTime > 0) ? remainingExpireTime :expire;
        }


        // 设置缓存，防止重复请求
        NaCacheTemplate.setCache(key, (cacheNum + 1), expire, TimeUnit.MILLISECONDS);

        // 继续执行被拦截的方法
        return pjp.proceed();
    }
}
