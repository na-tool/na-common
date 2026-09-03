package com.na.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.TransportMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * RedissonClient 配置类: 向 Spring 容器注册 {@link RedissonClient} Bean
 * <p>
 * Redisson 是基于 Redis 的高阶客户端, 提供 RedisTemplate 不具备的分布式能力:
 * 分布式锁(RLock)、分布式集合、布隆过滤器、限流器、延迟队列、分布式调度等。
 * <p>
 * 为什么需要手动配置: 本项目依赖的是 {@code redisson-spring-data-35:3.52.0}(对应 Spring Data Redis
 * 3.5 / Spring Boot 3.5 的桥接包), 而非 {@code redisson-spring-boot-starter}(后者会自动配置
 * RedissonClient)。因此需在此手动从 {@link RedisProperties} 读取连接信息创建客户端。
 * <p>
 * 生效条件(默认不开启, 需同时满足, 缺一不可):
 * <ol>
 *     <li>{@code spring.data.redis.host} 已配置且非空</li>
 *     <li>{@code na.redisson.enable=true} 显式开启(缺省视为 false)</li>
 * </ol>
 * <p>
 * 注意: Spring Boot 3.x 的 {@link RedisProperties} 前缀已从 {@code spring.redis} 改为
 * {@code spring.data.redis}; 同时移除了 {@code isSsl()}, 改用 {@code getSsl().isEnabled()}
 * 判断 SSL。故本配置条件以 {@code spring.data.redis.host} 为准。
 * <p>
 * 配置示例(application.yml):
 * <pre>{@code
 * spring:
 *   data:
 *     redis:
 *       host: 127.0.0.1
 *       port: 6379
 *       password:                # 无密码可留空
 *       ssl:
 *         enabled: false         # 启用 SSL 改为 true, 地址协议自动用 rediss://
 * na:
 *   redisson:
 *     enable: true               # 显式开启, 缺省为 false
 *     timeout: 3000             # 命令超时(毫秒), 默认 3000
 *     retry-attempts: 3         # 命令失败重试次数, 默认 3
 *     retry-interval: 1500      # 重试间隔(毫秒), 默认 1500
 * }</pre>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.redisson.enable:false}'.equals('true')")
public class RedissonConfig {

    /** 命令超时(毫秒), 默认 3000 */
    @Value("${na.redisson.timeout:3000}")
    private int timeout;

    /** 命令失败重试次数, 默认 3 */
    @Value("${na.redisson.retry-attempts:3}")
    private int retryAttempts;

    /** 重试间隔(毫秒), 默认 1500 */
    @Value("${na.redisson.retry-interval:1500}")
    private int retryInterval;

    /**
     * 注册名为 redissonClient 的 RedissonClient Bean
     * <p>
     * 使用单节点模式, 复用 Spring Boot 已装配的 {@link RedisProperties} 连接信息(host/port/password/ssl),
     * 避免业务方为 Redisson 重复配置一份 Redis 连接参数。传输模式默认 NIO(非阻塞)。
     * <p>
     * 示例: 由 Spring 容器自动装配, 业务方注入后使用分布式锁等高级 API
     * <pre>{@code
     * @Autowired
     * private RedissonClient redissonClient;
     *
     * // 可重入分布式锁
     * RLock lock = redissonClient.getLock("order:lock:1001");
     * if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
     *     try { /* 业务逻辑 *\/ } finally { lock.unlock(); }
     * }
     * }</pre>
     *
     * @param redisProperties Spring Boot 装配的 Redis 连接配置(读 spring.data.redis.* )
     * @return 配置好连接与重试策略的 RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        // 传输模式: NIO 非阻塞, 适合大多数场景; 高并发可改 TransportMode.EPOLL(Linux + netty-transport-native-epoll)
        config.setTransportMode(TransportMode.NIO);

        SingleServerConfig singleServerConfig = config.useSingleServer();

        // SSL: Spring Boot 3 移除了 RedisProperties.isSsl(), 改用 getSsl().isEnabled();
        // 启用 SSL 时协议用 rediss://, 否则 redis://
        boolean sslEnabled = redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled();
        String protocol = sslEnabled ? "rediss://" : "redis://";
        singleServerConfig.setAddress(protocol + redisProperties.getHost() + ":" + redisProperties.getPort());

        // 密码: 用 StringUtils.hasText 兼容 null 与空白串, 避免设置空密码导致鉴权异常
        String password = redisProperties.getPassword();
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }

        // database: 复用 RedisProperties 的库号, 避免 Redisson 与 RedisTemplate 操作不同的库导致数据错位
        singleServerConfig.setDatabase(redisProperties.getDatabase());

        // 超时与重试: 默认值可通过配置覆盖, 适配不同网络质量环境
        singleServerConfig.setTimeout(timeout);
        singleServerConfig.setRetryAttempts(retryAttempts);
        singleServerConfig.setRetryInterval(retryInterval);

        return Redisson.create(config);
    }
}
