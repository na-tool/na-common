package com.na.common.aop;

import com.na.common.annotation.NoRepeatSubmitIP;
import com.na.common.constant.IGlobalConst;
import com.na.common.exceptions.BusinessException;
import com.na.common.result.enums.Status;
import com.na.common.utils.AddressUtil;
import com.na.common.utils.CacheTemplateUtil;
import com.na.common.utils.CommonUtil;
import com.na.common.utils.DateTimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 防 IP 重复提交 AOP 拦截器
 * <p>
 * 基于 Redis 计数: 同一 IP + 请求路径 + (可选)请求参数, 在指定时间窗口内提交次数超过 {@code num} 时拒绝
 * <p>
 * 配置示例:
 * <pre>{@code
 * # application.yml
 * spring:
 *   data:
 *     redis:
 *       host: 127.0.0.1
 * na:
 *   submit:
 *     enable: true
 * }</pre>
 * <p>
 * 使用示例:
 * <pre>{@code
 * @NoRepeatSubmitIP(value = 5000, num = 3, showRemainingTime = true)
 * @PostMapping("/submit")
 * public Result submit() { ... }
 * // 5 秒内允许提交 3 次, 超出后提示剩余时间
 * }</pre>
 */
@Aspect
@Component
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.submit.enable:false}'.equals('true')")
public class NoRepeatSubmitIPAOP {

    @Autowired
    private CacheTemplateUtil cacheTemplateUtil;

    /**
     * 切点: 标注了 {@link NoRepeatSubmitIP} 的方法
     */
    @Pointcut("@annotation(com.na.common.annotation.NoRepeatSubmitIP)")
    public void pointCut() {
    }

    /**
     * 环绕通知: 校验同一 IP 在时间窗口内的提交次数
     * <p>
     * 流程:
     * <ol>
     *   <li>构建防重 key: IP + 认证头 + 请求路径 + (可选)请求参数</li>
     *   <li>从 Redis 读取当前提交计数</li>
     *   <li>若计数 >= num, 抛 {@link BusinessException} 拒绝请求</li>
     *   <li>否则计数 +1, 用剩余 TTL 或原始过期时间重新写入缓存</li>
     * </ol>
     *
     * @param pjp 切入点
     * @return 被拦截方法的返回值
     * @throws Throwable 计数超限抛 {@link BusinessException}; 被拦截方法异常时原样抛出
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = CommonUtil.getCurrentHttpRequest();

        // 构建 key: IP + 认证头 + 请求路径 + (可选)请求参数
        String key = buildKey(request, pjp);

        // 读取注解配置
        NoRepeatSubmitIP annotation = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(NoRepeatSubmitIP.class);
        long expire = annotation.value();
        int num = annotation.num();
        boolean showRemainingTime = annotation.showRemainingTime();

        // 从 Redis 读取当前提交计数
        int cacheNum = 0;
        Integer cache = cacheTemplateUtil.getCache(key, Integer.class);
        if (cache != null) {
            cacheNum = cache;
            Long remainingExpireTime = cacheTemplateUtil.getRemainingExpireTime(key);

            // 超过次数限制, 拒绝请求
            if (cacheNum >= num) {
                log.warn("重复请求拦截: {}", key);
                String msg = buildRejectMessage(annotation, remainingExpireTime, showRemainingTime);
                throw new BusinessException(msg, HttpStatus.OK);
            }

            // 未超限: 继续使用剩余 TTL, 保持原有窗口边界
            if (remainingExpireTime != null && remainingExpireTime > 0) {
                expire = remainingExpireTime;
            }
        }

        // 计数 +1, 刷新/设置缓存
        cacheTemplateUtil.setCache(key, cacheNum + 1, expire, TimeUnit.MILLISECONDS);

        return pjp.proceed();
    }

    /**
     * 构建防重 key: 前缀 + IP + 认证头 + 请求路径 + (可选)请求参数
     *
     * @param request HTTP 请求
     * @param pjp     切入点(用于读取 {@link NoRepeatSubmitIP#requestParams()})
     * @return Redis 缓存 key
     */
    private String buildKey(HttpServletRequest request, ProceedingJoinPoint pjp) {
        String ip = AddressUtil.getIpAddress(request);
        String authentication = request.getHeader(IGlobalConst.SECURITY.HEADER_STRING);
        if (!StringUtils.hasText(authentication)) {
            authentication = "auth";
        }
        String key = ip + authentication + "-" + request.getServletPath();

        // 可选: 追加请求参数, 让不同参数的请求分别计数
        NoRepeatSubmitIP annotation = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(NoRepeatSubmitIP.class);
        if (annotation.requestParams()) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            StringBuilder sb = new StringBuilder();
            parameterMap.forEach((k, v) -> sb.append(k).append(Arrays.toString(v)));
            key = key + "-" + sb;
        }
        return IGlobalConst.CACHE.REPEATED_SUBMIT_KEY + key;
    }

    /**
     * 构建拒绝消息: 基础消息 + (可选)剩余时间
     *
     * @param annotation          注解配置
     * @param remainingExpireTime 剩余过期时间(毫秒), 可能为 null
     * @param showRemainingTime   是否追加剩余时间
     * @return 拒绝消息
     */
    private String buildRejectMessage(NoRepeatSubmitIP annotation, Long remainingExpireTime, boolean showRemainingTime) {
        String msg = annotation.msg();
        if (showRemainingTime && remainingExpireTime != null && remainingExpireTime > 0) {
            msg = msg + annotation.dateStartMsg()
                    + DateTimeUtil.parseToString(remainingExpireTime, annotation.dateFormat())
                    + annotation.dateEndMsg();
        }
        return StringUtils.hasText(msg) ? msg : Status.FAIL_REQUEST_REPETITION.getMsg();
    }
}
