package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化语言工具类
 * <p>
 * 依赖 Spring 的 {@link MessageSource} 读取翻译文件(资源位置见 WebI18nConfigure:
 * classpath:static/i18n/messages*.properties), 语言环境默认中文(zh-CN),
 * 可通过请求头 Accept-Language 或 ?lang= 参数切换
 * <p>
 * 示例:
 * <pre>{@code
 * // 取翻译, key 不存在时返回 key 本身
 * String msg = LocalLangUtil.get("server.error");
 * // 取翻译, key 不存在时返回指定默认值(推荐, 配合枚举内置英文兜底)
 * String msg = LocalLangUtil.get("server.error", "server error");
 * }</pre>
 */
@Slf4j
@Component
public class LocalLangUtil {

    /** 由构造器注入的容器 MessageSource; 非 Spring 环境下为 null, get 方法会走兜底逻辑 */
    private static MessageSource messageSource;

    /**
     * 构造注入: 利用实例注入为静态字段赋值(本类为纯静态调用工具)
     *
     * @param messageSource 容器中的 MessageSource(Spring Boot 默认或 WebI18nConfigure 提供的实例)
     */
    public LocalLangUtil(MessageSource messageSource) {
        LocalLangUtil.messageSource = messageSource;
    }

    /**
     * 获取单个国际化翻译值
     * <p>
     * 翻译不存在时返回 msgKey 本身(便于排查缺失的翻译条目)
     *
     * @param msgKey 消息 key(对应 static/i18n/messages*.properties 中的条目)
     * @return 当前语言翻译值; key 不存在时返回 msgKey
     */
    public static String get(String msgKey) {
        return get(msgKey, null);
    }

    /**
     * 获取单个国际化翻译值, 支持自定义默认值
     * <p>
     * 示例: {@code String msg = LocalLangUtil.get("auth.expired", "The authorization is invalid");}
     *
     * @param msgKey        消息 key
     * @param defaultMessage 默认值: 翻译不存在或 MessageSource 不可用时返回该值; 传 null 则退化为返回 msgKey
     * @return 当前语言翻译值; key 不存在时返回 defaultMessage(为 null 时返回 msgKey)
     */
    public static String get(String msgKey, String defaultMessage) {
        // 非 Spring 环境或容器未注入时直接走默认值, 避免 NPE
        if (messageSource == null) {
            log.debug("MessageSource 未初始化, 返回默认值: msgKey={}", msgKey);
            return defaultMessage != null ? defaultMessage : msgKey;
        }
        try {
            Locale locale = LocaleContextHolder.getLocale();
            // defaultMessage 非 null 时, key 缺失由 MessageSource 直接返回默认值, 不抛异常
            String result = messageSource.getMessage(msgKey, null, defaultMessage, locale);
            log.debug("国际化取值: msgKey={}, locale={}, result={}", msgKey, locale, result);
            return result;
        } catch (Exception e) {
            // 翻译缺失(defaultMessage 为 null)等异常场景, 返回默认值或 msgKey 本身
            log.debug("获取国际化翻译值失败: msgKey={}", msgKey, e);
            return defaultMessage != null ? defaultMessage : msgKey;
        }
    }

    /**
     * 设置当前线程的语言环境(通常由 WebI18nConfigure 的拦截器在每个请求开始时调用)
     * <p>
     * 示例:
     * <pre>{@code
     * LocalLangUtil.setLocale("zh_CN");   // 中文
     * LocalLangUtil.setLocale("en-US");   // 英文(下划线/连字符均支持)
     * }</pre>
     *
     * @param language 语言标签, 支持 "zh_CN"/"zh-cn"/"en" 等格式; 空值忽略
     */
    public static void setLocale(String language) {
        if (language == null || language.trim().isEmpty()) {
            return;
        }
        // 统一为 IETF 语言标签格式: "zh_CN" -> "zh-cn" -> Locale
        Locale locale = Locale.forLanguageTag(language.trim().replace('_', '-').toLowerCase());
        LocaleContextHolder.setLocale(locale);
        log.debug("设置当前语言环境: {}", locale);
    }
}
