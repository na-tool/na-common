package com.na.common.config;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 作用
 * 配置并创建 {@code RedisTemplate<String, Object>} 实例，作为 Spring Data Redis 的核心操作模板。
 * RedisTemplate 是 Spring 提供的对 Redis 最基础的操作封装，支持字符串、对象存储、哈希、列表等常用操作。
 * 适合一般 Redis 缓存、简单的 key-value 数据存储。
 * 关键点
 * 依赖 Spring Data Redis 和 FastJSON（用于序列化）。
 * 使用 RedisConnectionFactory 管理连接。
 * 定制序列化器，key 使用字符串序列化器，value 和 hash value 使用 FastJsonRedisSerializer 进行 JSON 序列化，方便存储和读取复杂对象。
 * 方便快速操作 Redis 数据。
 */

@Configuration
@ConditionalOnProperty(name = {"spring.redis.host", "na.redis"}, matchIfMissing = false)
public class NaRedisConfig {

    @Bean
    // 定义一个名为 redisTemplate 的 Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 配置 Key 和 Value 的序列化方式
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new FastJsonRedisSerializer<>(Object.class));

        // 配置 Hash Key 和 Hash Value 的序列化方式
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new FastJsonRedisSerializer<>(Object.class));

        // 返回配置好的 RedisTemplate 实例
        return redisTemplate;
    }

}
