package com.na.common.aop;

import com.na.common.annotation.NoRepeatSubmitIP;
import com.na.common.utils.AddressUtil;
import com.na.common.utils.CacheTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * 防 IP 重复提交 AOP 拦截器
 * <p>
 * 基于 Redis 计数: 同一 IP + 请求路径 + (可选)请求参数, 在指定时间窗口内提交次数超过 {@code num} 时拒绝。
 * <p>
 * <b>本类只依赖客户端 IP 与请求路径, 不需要宿主提供任何用户体系实现</b>
 * （这正是它与 {@link NoRepeatSubmitAOP} 的区别）。因此它在未登录、无用户体系的场景也能独立生效:
 * 只要 {@code na.submit.enable=true} 且配置了 Redis 本切面就会注册，与
 * {@code INoRepeatSubmitUserService} 是否存在无关。
 * <p>
 * 公共流程与可用性约定见 {@link AbstractRepeatSubmitAspect}。
 * <p>
 * 配置示例:
 * <pre>{@code
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
@Slf4j
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.submit.enable:false}'.equals('true')")
public class NoRepeatSubmitIPAOP extends AbstractRepeatSubmitAspect {

    /**
     * @param cacheTemplateUtil Redis 缓存工具
     */
    public NoRepeatSubmitIPAOP(CacheTemplateUtil cacheTemplateUtil) {
        super(cacheTemplateUtil);
    }

    /**
     * 切点: 标注了 {@link NoRepeatSubmitIP} 的方法
     */
    @Pointcut("@annotation(com.na.common.annotation.NoRepeatSubmitIP)")
    public void pointCut() {
    }

    /**
     * 环绕通知: 校验同一 IP 在时间窗口内的提交次数
     *
     * @param pjp 切入点
     * @return 被拦截方法的返回值
     * @throws Throwable 计数超限抛业务异常; 被拦截方法异常时原样抛出
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        NoRepeatSubmitIP annotation = ((MethodSignature) pjp.getSignature())
                .getMethod().getAnnotation(NoRepeatSubmitIP.class);
        // 计数主体 = 客户端 IP; AddressUtil 取不到时返回空串, 公共骨架会据此放行并告警
        return checkAndProceed(pjp, RepeatSubmitRule.from(annotation), AddressUtil::getIpAddress);
    }

    @Override
    protected String aspectName() {
        return "NoRepeatSubmitIP";
    }

    @Override
    protected String blankSubjectMessage() {
        return "无法解析客户端 IP, 基于 IP 的防重复提交对这些请求已失效; 请检查代理头配置(见 AddressUtil)。";
    }
}
