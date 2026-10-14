package com.na.common.aop;

import com.na.common.annotation.NoRepeatSubmit;
import com.na.common.annotation.NoRepeatSubmitIP;
import com.na.common.utils.DateTimeUtil;

/**
 * 一次防重复提交校验的规则参数。
 *
 * <p>{@code @NoRepeatSubmit}（按用户 ID）与 {@code @NoRepeatSubmitIP}（按 IP）的注解属性
 * 完全一致，只有"计数主体怎么取"不同。用它把属性从具体注解类型上摘下来，
 * 使 {@link AbstractRepeatSubmitAspect} 能承载两者的公共流程。
 *
 * @param expire            时间窗口（毫秒）
 * @param num               窗口内允许的提交次数
 * @param showRemainingTime 是否在拒绝文案后追加剩余时间
 * @param msg               拒绝提示文案
 * @param dateStartMsg      剩余时间前缀
 * @param dateEndMsg        剩余时间后缀
 * @param dateFormat        剩余时间格式
 * @param requestParams     是否把请求参数并入 key
 */
record RepeatSubmitRule(long expire, int num, boolean showRemainingTime, String msg,
                        String dateStartMsg, String dateEndMsg,
                        DateTimeUtil.DateFormat dateFormat, boolean requestParams) {

    /**
     * 由用户维度注解构造
     *
     * @param annotation 注解实例
     * @return 规则参数
     */
    static RepeatSubmitRule from(NoRepeatSubmit annotation) {
        return new RepeatSubmitRule(annotation.value(), annotation.num(), annotation.showRemainingTime(),
                annotation.msg(), annotation.dateStartMsg(), annotation.dateEndMsg(),
                annotation.dateFormat(), annotation.requestParams());
    }

    /**
     * 由 IP 维度注解构造
     *
     * @param annotation 注解实例
     * @return 规则参数
     */
    static RepeatSubmitRule from(NoRepeatSubmitIP annotation) {
        return new RepeatSubmitRule(annotation.value(), annotation.num(), annotation.showRemainingTime(),
                annotation.msg(), annotation.dateStartMsg(), annotation.dateEndMsg(),
                annotation.dateFormat(), annotation.requestParams());
    }
}
