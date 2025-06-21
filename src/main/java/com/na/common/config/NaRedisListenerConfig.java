package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = {"spring.redis.host", "na.redisListener"}, matchIfMissing = false)
public class NaRedisListenerConfig {

    /**
     * 用于监听 Redis 消息队列中的消息，通常用于实现订阅（Pub/Sub）模式或处理消息过期等场景
     * @param connectionFactory Redis 连接工厂
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    // 定义一个名为 container 的 Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        // 创建一个 RedisMessageListenerContainer 实例
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // 设置连接工厂
        container.setConnectionFactory(connectionFactory);
        // 返回配置好的 RedisMessageListenerContainer 实例
        return container;
    }
}
