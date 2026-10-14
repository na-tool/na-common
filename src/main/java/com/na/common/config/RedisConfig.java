package com.na.common.config;

import com.alibaba.fastjson2.support.spring6.data.redis.FastJsonRedisSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类: 向 Spring 容器注册 {@code RedisTemplate<String, Object>} Bean
 * <p>
 * 序列化策略:
 * <ul>
 *     <li>Key / HashKey: {@link StringRedisSerializer}, 保证 key 在 redis-cli 中可读、无转义噪声</li>
 *     <li>Value / HashValue: {@link FastJsonRedisSerializer}, 以 JSON 存储复杂对象</li>
 * </ul>
 * <p>
 * 生效条件(默认不开启, 需同时满足, 缺一不可):
 * <ol>
 *     <li>{@code spring.data.redis.host} 已配置且非空(表示已接入 Redis)</li>
 *     <li>{@code na.redis.enable=true} 显式开启本配置(缺省视为 false)</li>
 * </ol>
 * 不满足时本 Bean 不注册, 回落到 Spring Boot 默认 {@code RedisAutoConfiguration}。
 * <p>
 * 注意: 原 {@code @ConditionalOnProperty(name={...})} 对多个 name 是 OR 语义且
 * {@code matchIfMissing=true} 会"缺失即开启", 与"默认关闭、显式开启"需求相反,
 * 故改用 {@link ConditionalOnExpression} 实现 AND 双条件。
 * <p>
 * 配置示例(application.yml):
 * <pre>{@code
 * spring:
 *   redis:
 *     host: 127.0.0.1
 *     port: 6379
 *     password:           # 无密码可留空
 * na:
 *   redis:
 *     enable: true        # 显式开启, 缺省为 false
 * }</pre>
 * <p>
 * FastJson2 反序列化说明: value 以 Object.class 反序列化时, 自定义类型会被解析为
 * {@code JSONObject} 而非原对象, 业务方取值后需自行 {@code JSON.toJavaObject(obj, TargetClass.class)}
 * 转回原类型; 如需自动还原类型, 建议改用 {@code Jackson2JsonRedisSerializer} 并启用默认类型注入。
 */
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty() && '${na.redis.enable:false}'.equals('true')")
public class RedisConfig {

    /**
     * 注册名为 redisTemplate 的 RedisTemplate Bean
     * <p>
     * 示例: 由 Spring 容器自动装配, 业务方通过 @Autowired 注入使用
     * <pre>{@code
     * @Autowired
     * private RedisTemplate<String, Object> redisTemplate;
     *
     * // 存对象
     * redisTemplate.opsForValue().set("user:1", userDto);
     * // 取对象(注意 FastJson2 会返回 JSONObject, 需手动转类型)
     * Object raw = redisTemplate.opsForValue().get("user:1");
     * UserDto user = JSON.toJavaObject(raw, UserDto.class);
     * }</pre>
     *
     * @param redisConnectionFactory Redis 连接工厂, 由 Spring Boot RedisAutoConfiguration 自动创建
     * @return 配置好序列化器的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Key 与 HashKey 用 String 序列化, 保证 key 可读且无二进制转义
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // Value 与 HashValue 用 FastJson 序列化, 以 JSON 存储复杂对象
        FastJsonRedisSerializer<Object> fastJsonSerializer = new FastJsonRedisSerializer<>(Object.class);
        redisTemplate.setValueSerializer(fastJsonSerializer);
        redisTemplate.setHashValueSerializer(fastJsonSerializer);

        // 触发模板初始化校验, 使序列化器等配置在返回前生效(否则首次调用前可能为未就绪状态)
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
