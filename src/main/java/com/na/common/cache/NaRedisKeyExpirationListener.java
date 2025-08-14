package com.na.common.cache;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 自定义 Redis Key 过期事件监听器（兼容阿里云 Redis，避免 CONFIG GET）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"spring.redis.host", "na.redisMessage"}, matchIfMissing = false)
public class NaRedisKeyExpirationListener implements MessageListener {

    @Autowired
    private RedisMessageListenerContainer listenerContainer;

    @Autowired
    @Lazy
    private INaRedisKeyExpirationService naRedisKeyExpirationService;

    /**
     * 手动订阅 key 过期事件
     */
    @PostConstruct
    public void init() {
        // __keyevent@*__:expired 代表所有 DB 的 key 过期事件
        listenerContainer.addMessageListener(this, new PatternTopic("__keyevent@*__:expired"));
        log.info("已启动 Redis Key 过期事件监听器");
    }

    /**
     * 处理 key 过期事件
     */
    @SneakyThrows
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("接收到失效消息 {}", expiredKey);
        try {
            if (naRedisKeyExpirationService != null) {
                naRedisKeyExpirationService.run(expiredKey, pattern);
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.info("没有可用的 Redis 过期消息处理实例");
        } finally {
            log.info("失效消息 {} 任务处理完成", expiredKey);
        }
    }
}
