package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 消息监听容器配置
 * <p>
 * 注册 {@link RedisMessageListenerContainer} Bean, 供订阅(Pub/Sub)或 Key 过期事件监听挂载 Listener。
 * 容器线程安全, 可挂载多个 Listener 各自订阅不同 Topic。
 * <p>
 * 生效条件: 配置了 {@code spring.data.redis.host} 且 {@code na.redisListener.enable=true}(默认关闭)
 * <p>
 * 配置示例(application.yml):
 * <pre>{@code
 * spring:
 *   data:
 *     redis:
 *       host: 127.0.0.1
 * na:
 *   redis-listener:
 *     enable: true
 * }</pre>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.redisListener.enable:false}'.equals('true')")
public class RedisListenerConfig {

    /**
     * 注册 Redis 消息监听容器
     * <p>
     * 业务方自行实现 {@link org.springframework.data.redis.connection.MessageListener} 后,
     * 注入此 container 调用 {@code addMessageListener(listener, topic)} 订阅指定频道
     * <p>
     * 示例:
     * <pre>{@code
     * @Autowired
     * private RedisMessageListenerContainer container;
     *
     * @PostConstruct
     * public void init() {
     *     // 订阅 order:paid 频道
     *     container.addMessageListener(myListener, new ChannelTopic("order:paid"));
     * }
     * }</pre>
     *
     * @param connectionFactory Redis 连接工厂(由 Spring Boot 自动装配)
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
