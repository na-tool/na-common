package com.na.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.TransportMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 作用
 * 配置并创建 RedissonClient 实例。
 * Redisson 是一个基于 Redis 的高阶客户端，提供了分布式锁、分布式集合、分布式队列、分布式缓存等高级功能。
 * 适合需要分布式协调、复杂数据结构或者分布式锁的场景。
 * 关键点
 * 依赖 Redisson 库。
 * 通过 RedisProperties 读取 Redis 连接配置（host、port、密码、是否SSL）。
 * 配置了连接的超时、重试策略。
 * 传输模式使用非阻塞的 NIO。
 * 只负责创建 RedissonClient，由用户代码通过这个客户端完成高级操作。
 */
@Configuration
@ConditionalOnProperty(name = {"spring.redis.host", "na.redissonClient"}, matchIfMissing = false)
public class NaRedissonConfig {
    @Bean
    // 定义一个名为 redissonClient 的 Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        // 创建一个 Redisson 的配置对象
        Config config = new Config();
        // 设置传输模式为 NIO（非阻塞 IO）
        config.setTransportMode(TransportMode.NIO);
        // 配置单节点服务器
        SingleServerConfig singleServerConfig = config.useSingleServer();
        // 可以用 "rediss://" 来启用 SSL 连接
        // 设置 Redis 服务器的地址，格式为 "redis://<host>:<port>"
        String protocol = redisProperties.isSsl() ? "rediss://" : "redis://";
        singleServerConfig.setAddress(protocol + redisProperties.getHost() + ":" + redisProperties.getPort());
        // 设置 Redis 服务器的密码
        // 如果存在 Redis 密码则设置
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            singleServerConfig.setPassword(redisProperties.getPassword());
        }

        // 设置连接超时、重试次数和重试间隔
        singleServerConfig.setTimeout(3000);
        singleServerConfig.setRetryAttempts(3);
        singleServerConfig.setRetryInterval(1500);

        // 创建并返回 RedissonClient 实例
        return Redisson.create(config);
    }
}
