package com.na.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class NaLocalLangUtil {
    private static MessageSource messageSource;

    public NaLocalLangUtil(MessageSource messageSource)
    {
        NaLocalLangUtil.messageSource = messageSource;
    }

    /**
     * 获取单个国际化翻译值
     * @param msgKey 消息key
     * @return 翻译值
     */
    public static String get(String msgKey)
    {
        try
        {
            Locale locale = LocaleContextHolder.getLocale();
            System.out.println("尝试获取国际化：" + msgKey + "，当前 locale: " + locale);
            String result = messageSource.getMessage(msgKey, null, locale);
            System.out.println("获取结果: " + result);
            return result;
        }
        catch (Exception e)
        {
            System.out.println("获取国际化翻译值失败：" + msgKey);
            System.out.println(e);
            return msgKey;
        }
    }

    public static void setLocale(String language){
        language = language.replace('_', '-');  // 替换下划线为连字符
        Locale locale = Locale.forLanguageTag(language);  // 使用有效的语言标签
        LocaleContextHolder.setLocale(locale);  // 设置当前 Locale
    }
}
