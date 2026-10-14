package com.na.common.aop;

import com.na.common.annotation.NoRepeatSubmit;
import com.na.common.aop.service.INoRepeatSubmitUserService;
import com.na.common.utils.CacheTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * 防重复提交 AOP 拦截器(基于用户 ID)
 * <p>
 * 与 {@link NoRepeatSubmitIPAOP} 区别: 本类基于用户 ID 而非 IP 构建防重 key, 适用于已登录场景。
 * 两者的公共流程与可用性约定见 {@link AbstractRepeatSubmitAspect}。
 * <p>
 * <b>依赖宿主提供 {@link INoRepeatSubmitUserService}</b>: 本类靠它取当前用户标识，
 * 而用户体系只能由宿主提供。因此注册阶段容器里没有该类型的 Bean 时，本切面<b>不注册</b>并打印
 * 一条 WARN —— 宿主只想要 IP 维度防重时不会因此启动失败；运行阶段实现返回空值时放行请求并告警。
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
 * @NoRepeatSubmit(value = 5000, num = 1, showRemainingTime = true)
 * @PostMapping("/order/create")
 * public Result createOrder() { ... }
 * // 5 秒内同一用户仅允许提交 1 次
 * }</pre>
 */
@Aspect
@Slf4j
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.submit.enable:false}'.equals('true')")
public class NoRepeatSubmitAOP extends AbstractRepeatSubmitAspect {

    private final INoRepeatSubmitUserService noRepeatSubmitUserService;

    /**
     * @param noRepeatSubmitUserService 宿主实现的当前用户标识提供者(注册前已确认存在)
     * @param cacheTemplateUtil         Redis 缓存工具
     */
    public NoRepeatSubmitAOP(INoRepeatSubmitUserService noRepeatSubmitUserService,
                             CacheTemplateUtil cacheTemplateUtil) {
        super(cacheTemplateUtil);
        this.noRepeatSubmitUserService = noRepeatSubmitUserService;
    }

    /**
     * 切点: 标注了 {@link NoRepeatSubmit} 的方法
     */
    @Pointcut("@annotation(com.na.common.annotation.NoRepeatSubmit)")
    public void pointCut() {
    }

    /**
     * 环绕通知: 校验同一用户在时间窗口内的提交次数
     *
     * @param pjp 切入点
     * @return 被拦截方法的返回值
     * @throws Throwable 计数超限抛业务异常; 被拦截方法异常时原样抛出
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        NoRepeatSubmit annotation = ((MethodSignature) pjp.getSignature())
                .getMethod().getAnnotation(NoRepeatSubmit.class);
        // 计数主体 = 当前用户标识
        return checkAndProceed(pjp, RepeatSubmitRule.from(annotation), request -> noRepeatSubmitUserService.userId());
    }

    @Override
    protected String aspectName() {
        return "NoRepeatSubmit";
    }

    @Override
    protected String blankSubjectMessage() {
        return "INoRepeatSubmitUserService.userId() 返回空, 基于用户 ID 的防重复提交已失效; 请检查宿主的实现。";
    }
}
