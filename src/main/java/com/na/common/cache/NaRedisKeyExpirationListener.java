package com.na.common.cache;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = {"spring.redis.host", "na.redisMessage"}, matchIfMissing = false)
public class NaRedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    @Autowired
    @Lazy
    private INaRedisKeyExpirationService naRedisKeyExpirationService;
    /**
     * Creates new {@link MessageListener} for {@code __keyevent@*__:expired} messages.
     *
     * @param listenerContainer must not be {@literal null}.
     */
    public NaRedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    /**
     * 针对redis数据失效事件，进行数据处理
     */
    @SneakyThrows
    @Override
    public void onMessage(Message message, byte[] pattern) {
        //获得失效的key
        String expiredKey = message.toString();
        log.info("接收到失效消息"+expiredKey);
        try {
            if (naRedisKeyExpirationService == null) return;
            naRedisKeyExpirationService.run(expiredKey, pattern);
        }catch (NoSuchBeanDefinitionException e){
            log.info("没有可用的Redis过期消息处理实例");
        } finally {
            log.info("失效消息{}任务处理完成",expiredKey);
        }
    }
}
