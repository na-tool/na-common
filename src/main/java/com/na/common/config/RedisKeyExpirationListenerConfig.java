package com.na.common.config;

import com.na.common.config.service.IRedisKeyExpirationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Key 过期事件监听配置
 * <p>
 * 订阅 {@code __keyevent@*__:expired} 模式, 接收所有 DB 的 key 过期事件,
 * 转交给 {@link IRedisKeyExpirationService} 实现类处理业务逻辑(如订单超时取消、缓存失效回源等)。
 * <p>
 * <b>前置条件(必须)</b>: Redis 需开启键空间通知, 执行 {@code CONFIG SET notify-keyspace-events Ex}
 * (或在 redis.conf 持久化配置)。未开启则收不到任何过期事件。
 * <p>
 * 生效条件: 配置了 {@code spring.data.redis.host} 且 {@code na.redisMessage.enable=true}(默认关闭)
 * <p>
 * 配置示例:
 * <pre>{@code
 * spring:
 *   data:
 *     redis:
 *       host: 127.0.0.1
 * na:
 *   redisMessage:
 *     enable: true
 *
 * // 业务侧实现处理逻辑
 * @Component
 * public class OrderKeyExpirationService implements IRedisKeyExpirationService {
 *     public void run(String key, byte[] pattern) throws Exception {
 *         if (key.startsWith("order:timeout:")) {
 *             orderService.cancel(key.substring("order:timeout:".length()));
 *         }
 *     }
 * }
 * }</pre>
 */
@Slf4j
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.redisMessage.enable:false}'.equals('true')")
public class RedisKeyExpirationListenerConfig implements MessageListener {

    /**
     * Redis 消息监听容器
     * <p>用 {@link ObjectProvider} 获取而非直接注入: 容器由 RedisListenerConfig 在
     * na.redisListener.enable 或 na.redisMessage.enable 时注册, 缺失时跳过订阅而不是启动失败
     */
    private final ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider;

    /**
     * @param listenerContainerProvider 监听容器提供者(可能为空)
     */
    public RedisKeyExpirationListenerConfig(ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider) {
        this.listenerContainerProvider = listenerContainerProvider;
    }

    /**
     * Key 过期业务处理器
     * <p>required=false: 业务未实现此接口时监听器仍可启动(仅记录日志不处理), 避免强依赖阻塞启动
     * <p>@Lazy: 延迟解析, 避免与业务 Bean 的潜在循环依赖
     */
    @Autowired(required = false)
    @Lazy
    private IRedisKeyExpirationService redisKeyExpirationService;

    /**
     * 启动时挂载 key 过期事件订阅
     */
    @PostConstruct
    public void init() {
        RedisMessageListenerContainer container = listenerContainerProvider.getIfAvailable();
        if (container == null) {
            log.warn("未找到 RedisMessageListenerContainer, 跳过 key 过期事件订阅; "
                    + "请确认已开启 na.redisListener.enable 或 na.redisMessage.enable");
            return;
        }
        // __keyevent@<db>__:expired: <db> 用 * 通配所有库; 需 Redis 开启 notify-keyspace-events Ex
        container.addMessageListener(this, new PatternTopic("__keyevent@*__:expired"));
        log.info("已启动 Redis Key 过期事件监听器, 订阅模式: __keyevent@*__:expired");
    }

    /**
     * 接收 key 过期事件并转交业务处理器
     * <p>异常被捕获并记录, 不向上抛出, 避免单条消息处理失败导致监听容器中断后续订阅
     *
     * @param message 过期 key 的消息体(toString 后为 key 字符串)
     * @param pattern 订阅模式(原始字节, 即 __keyevent@*__:expired)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("接收到 Redis Key 过期事件: {}", expiredKey);

        // 无业务处理器时仅记录, 不报错(允许只监听不处理的场景)
        if (redisKeyExpirationService == null) {
            log.debug("无 IRedisKeyExpirationService 实现, 跳过处理: {}", expiredKey);
            return;
        }

        try {
            redisKeyExpirationService.run(expiredKey, pattern);
        } catch (Exception e) {
            // 业务处理异常不中断监听, 仅记录; 避免单条失败影响后续过期事件接收
            log.error("处理 Redis Key 过期事件失败: key={}", expiredKey, e);
        }
    }
}
