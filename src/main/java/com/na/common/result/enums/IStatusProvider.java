package com.na.common.result.enums;

/**
 * 状态提供者接口: 枚举响应状态(如 {@link Status})需实现本接口, 供 {@code Result} 统一封装响应
 * <p>
 * 语言取值有三套机制, 优先级与用途如下:
 * <ol>
 *   <li>{@link #getMsg()}: 不依赖 i18n 文件, 按 Session 语言直接返回内置中文/英文(轻量、永远可用)</li>
 *   <li>{@link #getLanguageMsg()}: 优先从 static/i18n/messages*.properties 读取翻译,
 *       key 不存在时回退内置英文(完整国际化推荐)</li>
 *   <li>{@link #msgKey()}: i18n 资源文件中的 key 约定</li>
 * </ol>
 */
public interface IStatusProvider {

    /**
     * 获取状态码
     *
     * @return 状态码(如 1 成功 / -1 失败 / 401 未登录)
     */
    Integer getCode();

    /**
     * 获取内置英文消息(同时作为 i18n 缺失时的兜底文案)
     *
     * @return 英文消息
     */
    String getEnMsg();

    /**
     * 获取内置中文消息
     *
     * @return 中文消息
     */
    String getZhMsg();

    /**
     * 按当前会话语言返回内置消息: 中文语言环境返回 {@link #getZhMsg()}, 其余返回 {@link #getEnMsg()}
     *
     * @return 当前语言的内置消息
     */
    String getMsg();

    /**
     * 获取 i18n 资源文件中的消息 key(对应 static/i18n/messages*.properties 中的条目)
     *
     * @return 消息 key, 如 "server.error"
     */
    String msgKey();

    /**
     * 获取当前语言的国际化消息: 优先从 i18n 资源文件按 msgKey 读取当前语言翻译;
     * 资源文件中不存在该 key 时, 回退返回内置英文消息 {@link #getEnMsg()}
     *
     * @return 当前语言的国际化消息(保证非空)
     */
    String getLanguageMsg();
}
