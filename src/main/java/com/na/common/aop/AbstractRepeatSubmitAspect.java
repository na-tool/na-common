package com.na.common.aop;

import com.na.common.constant.IGlobalConst;
import com.na.common.exceptions.BusinessException;
import com.na.common.result.enums.Status;
import com.na.common.utils.CacheTemplateUtil;
import com.na.common.utils.CommonUtil;
import com.na.common.utils.DateTimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 防重复提交切面的公共骨架。
 *
 * <p>{@link NoRepeatSubmitAOP}（按用户 ID）与 {@link NoRepeatSubmitIPAOP}（按 IP）
 * 只在"计数主体怎么取"和注解类型上不同，其余流程完全一致：
 * 请求上下文检查 → 组装 key → 原子自增 → 超限拒绝。
 * 抽出前这段流程在两个类里各有约 80 行逐字重复的实现。
 *
 * <p><b>可用性约定集中在这里</b>：防重是"尽力而为"的辅助能力，以下情况一律放行请求并记录日志，
 * 绝不因为防重组件自身的问题阻断业务：
 * <ul>
 *   <li>没有 HTTP 请求上下文（定时任务、MQ 消费里调用被标注的方法）</li>
 *   <li>计数主体取不到（用户标识为空、解析不出客户端 IP）——
 *       此时会让所有请求共用一个 key 而互相误伤，放行比误伤更安全</li>
 *   <li>计数后端（Redis）不可用</li>
 * </ul>
 *
 * <p>反之，计数确实超限时会抛 {@link BusinessException}，由全局异常处理器转成统一响应。
 * 拒绝文案的格式化失败也不允许把"拒绝"变成"500"，会退回不带剩余时间的提示。
 */
abstract class AbstractRepeatSubmitAspect {

    private static final Logger log = LoggerFactory.getLogger(AbstractRepeatSubmitAspect.class);

    private final CacheTemplateUtil cacheTemplateUtil;

    /** 计数主体取不到时只告警一次，避免每请求刷屏 */
    private final AtomicBoolean blankSubjectWarned = new AtomicBoolean(false);

    /**
     * @param cacheTemplateUtil Redis 缓存工具
     */
    protected AbstractRepeatSubmitAspect(CacheTemplateUtil cacheTemplateUtil) {
        this.cacheTemplateUtil = cacheTemplateUtil;
    }

    /**
     * 计数主体解析器：从请求中取出用于计数的标识（用户 ID 或客户端 IP）
     */
    @FunctionalInterface
    protected interface SubjectResolver {

        /**
         * @param request 当前 HTTP 请求
         * @return 计数主体；返回 null 或空白表示无法确定，调用方会放行请求
         */
        String resolve(HttpServletRequest request);
    }

    /**
     * 切面名称，仅用于日志区分两个切面
     *
     * @return 切面名称
     */
    protected abstract String aspectName();

    /**
     * 计数主体取不到时的告警文案
     *
     * @return 告警文案
     */
    protected abstract String blankSubjectMessage();

    /**
     * 模板方法：完成一次防重校验，通过则执行目标方法
     *
     * @param pjp      切入点
     * @param rule     规则参数
     * @param resolver 计数主体解析器
     * @return 被拦截方法的返回值
     * @throws Throwable 计数超限抛 {@link BusinessException}；被拦截方法异常时原样抛出
     */
    protected Object checkAndProceed(ProceedingJoinPoint pjp, RepeatSubmitRule rule, SubjectResolver resolver)
            throws Throwable {
        HttpServletRequest request = CommonUtil.getCurrentHttpRequest();
        if (request == null) {
            log.debug("[{}] 无 HTTP 请求上下文, 跳过防重复提交校验: {}",
                    aspectName(), pjp.getSignature().toShortString());
            return pjp.proceed();
        }

        String subject = resolver.resolve(request);
        if (!StringUtils.hasText(subject)) {
            if (blankSubjectWarned.compareAndSet(false, true)) {
                log.warn("[{}] {} 同一提示只打印一次。", aspectName(), blankSubjectMessage());
            }
            return pjp.proceed();
        }

        String key = buildKey(request, subject, rule);

        // 原子自增并首次设置窗口过期时间。
        // 原实现是 get → 比较 → set 三步, 并发下两个请求会读到同一个计数值并同时放行,
        // 恰好在最需要防重的临界点上失效。
        Long count = cacheTemplateUtil.incrWithWindow(key, rule.expire());
        if (count == null) {
            // Redis 不可用时放行, 不因防重组件故障阻断业务
            log.warn("[{}] 防重计数不可用, 放行请求: {}", aspectName(), key);
            return pjp.proceed();
        }

        if (count > rule.num()) {
            log.warn("[{}] 重复请求拦截: {}", aspectName(), key);
            Long remainingExpireTime = cacheTemplateUtil.getRemainingExpireTime(key);
            throw new BusinessException(buildRejectMessage(rule, remainingExpireTime), HttpStatus.OK);
        }

        return pjp.proceed();
    }

    /**
     * 组装防重 key：前缀 + 计数主体 + 认证头 + 请求路径 + (可选)请求参数
     *
     * @param request HTTP 请求
     * @param subject 计数主体（用户 ID 或 IP）
     * @param rule    规则参数
     * @return Redis 缓存 key
     */
    private static String buildKey(HttpServletRequest request, String subject, RepeatSubmitRule rule) {
        String authentication = request.getHeader(IGlobalConst.SECURITY.HEADER_STRING);
        if (!StringUtils.hasText(authentication)) {
            authentication = "auth";
        }
        String key = subject + authentication + "-" + request.getServletPath();

        // 可选: 追加请求参数, 让不同参数的请求分别计数
        if (rule.requestParams()) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            StringBuilder sb = new StringBuilder();
            parameterMap.forEach((k, v) -> sb.append(k).append(Arrays.toString(v)));
            key = key + "-" + sb;
        }
        return IGlobalConst.CACHE.REPEATED_SUBMIT_KEY + key;
    }

    /**
     * 构建拒绝消息：基础消息 + (可选)剩余时间
     *
     * @param rule                规则参数
     * @param remainingExpireTime 剩余过期时间(毫秒), 可能为 null
     * @return 拒绝消息
     */
    private static String buildRejectMessage(RepeatSubmitRule rule, Long remainingExpireTime) {
        String msg = rule.msg();
        if (rule.showRemainingTime() && remainingExpireTime != null && remainingExpireTime > 0) {
            try {
                msg = msg + rule.dateStartMsg()
                        + DateTimeUtil.parseToString(remainingExpireTime, rule.dateFormat())
                        + rule.dateEndMsg();
            } catch (Exception e) {
                // 时间格式化失败不应把"拒绝"变成"500", 退回不带剩余时间的提示
                log.warn("格式化剩余时间失败, 使用不含剩余时间的提示文案", e);
            }
        }
        return StringUtils.hasText(msg) ? msg : Status.FAIL_REQUEST_REPETITION.getMsg();
    }
}
