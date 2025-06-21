package com.na.common.cache;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.common.utils.NaSpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class NaCacheTemplate {

    // 获取 Spring 容器中的 StringRedisTemplate Bean
    private static final StringRedisTemplate redisTemplate = NaSpringContextUtil.getBean(StringRedisTemplate.class);

    private static final ValueOperations<String, String> valueRedis;

    static {
        valueRedis = redisTemplate.opsForValue();
    }

    /**
     * 删除指定缓存键
     * @param key 缓存键
     */
    public static void clear(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("clear skipped: key is blank");
            return;
        }
        redisTemplate.delete(key);
    }

    /**
     * 删除所有以 prefix 开头的缓存键
     * @param prefix 键前缀
     */
    public static void clearByPrefix(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            log.warn("clearByPrefix skipped: prefix is blank");
            return;
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 删除所有包含特定字符串的缓存键
     * @param pattern 键模式
     */
    public static void clearKeysWithPattern(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            log.warn("clearKeysWithPattern skipped: pattern is blank");
            return;
        }
        Set<String> keys = redisTemplate.keys("*" + pattern + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Deleted keys: {}", keys);
        } else {
            log.info("No keys found matching pattern: {}", pattern);
        }
    }

    /**
     * 存储对象为 JSON 字符串到 Redis 缓存（无过期时间）
     * @param key 缓存键
     * @param obj 缓存对象
     * @param <T> 对象类型
     */
    public static <T> void setCache(String key, T obj) {
        if (StringUtils.isBlank(key)) {
            log.warn("setCache skipped: key is blank");
            return;
        }
        valueRedis.set(key, JSONObject.toJSONString(obj));
    }

    /**
     * 存储对象为 JSON 字符串到 Redis 缓存，并设置过期时间
     * @param key 缓存键
     * @param obj 缓存对象
     * @param time 过期时间
     * @param timeUnit 时间单位
     * @param <T> 对象类型
     */
    public static <T> void setCache(String key, T obj, Long time, TimeUnit timeUnit) {
        if (StringUtils.isBlank(key)) {
            log.warn("setCache with expire skipped: key is blank");
            return;
        }
        if (time == null || time <= 0) {
            setCache(key, obj);
            return;
        }
        valueRedis.set(key, JSONObject.toJSONString(obj), time, timeUnit);
    }

    /**
     * 获取缓存中的字符串值
     * @param key 缓存键
     * @return JSON字符串，找不到返回 null
     */
    public static String getCache(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("getCache skipped: key is blank");
            return null;
        }
        return valueRedis.get(key);
    }

    /**
     * 获取指定类型的缓存对象
     * @param key 缓存键
     * @param tClass 目标类型 Class
     * @param <T> 泛型类型
     * @return 目标类型对象，找不到或解析失败返回 null
     */
    public static <T> T getCache(String key, Class<T> tClass) {
        String cache = getCache(key);
        if (cache == null) {
            return null;
        }
        try {
            return JSONObject.parseObject(cache, tClass);
        } catch (Exception e) {
            log.error("JSON parse error for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定类型缓存，如果不存在则调用回调函数获取数据
     * @param key 缓存键
     * @param tClass 目标类型 Class
     * @param function 缓存未命中时调用的函数
     * @param <T> 泛型类型
     * @return 缓存数据或回调结果
     */
    public static <T> T getCache(String key, Class<T> tClass, Supplier<T> function) {
        T cache = getCache(key, tClass);
        if (cache != null) {
            return cache;
        }
        return function.get();
    }

    /**
     * 获取指定类型的缓存列表
     * @param key 缓存键
     * @param tClass 列表元素类型
     * @param <T> 元素泛型
     * @return List，缓存为空返回空列表
     */
    public static <T> List<T> getCacheList(String key, Class<T> tClass) {
        String cache = getCache(key);
        if (cache == null) {
            return Collections.emptyList();
        }
        try {
            return JSONArray.parseArray(cache, tClass);
        } catch (Exception e) {
            log.error("JSON parseArray error for key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定类型的缓存集合
     * @param key 缓存键
     * @param clazz 元素类型 Class
     * @param <T> 原始缓存类型
     * @return Set，找不到或解析失败返回 null
     */
    public static <T> Set<T> getCacheSet(String key, Class<T> clazz) {
        String cache = getCache(key);
        if (cache == null) {
            return null;
        }
        try {
            return JSONObject.parseObject(cache, new TypeReference<Set<T>>() {});
        } catch (Exception e) {
            log.error("JSON parse Set error for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定类型数据，如果没有则执行 function，结果转成 @return {@code List<B>}
     * @param key 缓存键
     * @param tClass 缓存数据的原始类型（通常为 {@code List.class }）
     * @param function 缓存未命中时回调函数
     * @param itemClass 最终列表元素类型
     * @param <T> 缓存数据类型泛型
     * @param <B> 最终元素类型泛型
     * @return {@code List<B>}
     */
    public static <T, B> List<B> getCacheList(String key, Class<T> tClass, Supplier<T> function, Class<B> itemClass) {
        T cache = getCache(key, tClass, function);
        List<B> result = new ArrayList<>();
        if (cache instanceof Collection) {
            try {
                List<B> bs = JSONObject.parseArray(JSONObject.toJSONString(cache), itemClass);
                result.addAll(bs);
            } catch (Exception e) {
                log.error("JSON parseArray error for key {}: {}", key, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 获取指定类型数据，如果没有则执行 function，结果转成 {@code Set<B>}
     * @param key 缓存键
     * @param tClass 缓存数据的原始类型
     * @param function 缓存未命中时回调函数
     * @param itemClass 最终元素类型
     * @param <T> 缓存数据泛型
     * @param <B> 最终元素泛型
     * @return {@code Set<B>}
     */
    public static <T, B> Set<B> getCacheSet(String key, Class<T> tClass, Supplier<T> function, Class<B> itemClass) {
        T cache = getCache(key, tClass, function);
        Set<B> result = new HashSet<>();
        if (cache instanceof Collection) {
            try {
                List<B> bs = JSONObject.parseArray(JSONObject.toJSONString(cache), itemClass);
                result.addAll(bs);
            } catch (Exception e) {
                log.error("JSON parseArray error for key {}: {}", key, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 获取指定类型数据，如果没有则执行 function，结果转成 {@code Deque<B>}
     * @param key 缓存键
     * @param tClass 缓存数据的原始类型
     * @param function 缓存未命中时回调函数
     * @param itemClass 元素类型
     * @param <T> 缓存数据泛型
     * @param <B> 元素泛型
     * @return {@code Deque<B>}
     */
    public static <T, B> Deque<B> getCacheDeque(String key, Class<T> tClass, Supplier<T> function, Class<B> itemClass) {
        T cache = getCache(key, tClass, function);
        Deque<B> result = new LinkedList<>();
        if (cache instanceof Collection) {
            try {
                List<B> bs = JSONObject.parseArray(JSONObject.toJSONString(cache), itemClass);
                result.addAll(bs);
            } catch (Exception e) {
                log.error("JSON parseArray error for key {}: {}", key, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 获取剩余过期时间，单位毫秒
     * @param key 缓存键
     * @return 剩余时间毫秒，null表示不存在，-1表示无过期时间
     */
    public static Long getRemainingExpireTime(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("getRemainingExpireTime skipped: key is blank");
            return null;
        }
        Long ttlMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        if (ttlMillis == null || ttlMillis == -2) {
            return null;
        } else if (ttlMillis == -1) {
            return -1L;
        }
        return ttlMillis;
    }

    /**
     * 判断缓存键是否存在
     * @param key 缓存键
     * @return true 存在，false 不存在或 key 为空
     */
    public static boolean exists(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Redis Hash 存值
     * @param key Redis 键
     * @param item Hash 字段名
     * @param value 字段值
     * @return 操作是否成功
     */
    public static boolean hset(String key, String item, Object value) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(item)) {
            log.warn("hset skipped: key or item is blank");
            return false;
        }
        try {
            redisTemplate.opsForHash().put(key, item, value);
            log.info("HSET - key: {}, item: {}, value: {}", key, item, value);
            return true;
        } catch (Exception e) {
            log.error("HSET error - key: {}, item: {}, value: {}", key, item, value, e);
            return false;
        }
    }

    /**
     * Redis Hash 存值并设置过期时间（秒）
     * @param key Redis 键
     * @param item Hash 字段名
     * @param value 字段值
     * @param time 过期时间，秒，&gt;0才生效
     * @return 操作是否成功
     */
    public static boolean hset(String key, String item, Object value, long time) {
        if (time <= 0) {
            return hset(key, item, value);
        }
        boolean success = hset(key, item, value);
        if (success) {
            return expire(key, time);
        }
        return false;
    }

    /**
     * 设置键过期时间（秒）
     * @param key 键
     * @param time 过期时间秒数，必须 &gt;0
     * @return 是否成功
     */
    public static boolean expire(String key, long time) {
        if (time <= 0 || StringUtils.isBlank(key)) {
            log.warn("expire skipped: time <= 0 or key is blank");
            return false;
        }
        try {
            boolean result = redisTemplate.expire(key, time, TimeUnit.SECONDS);
            log.info("Set expire - key: {}, time: {}s, result: {}", key, time, result);
            return result;
        } catch (Exception e) {
            log.error("Expire error - key: {}, time: {}", key, time, e);
            return false;
        }
    }

    /**
     * 获取 Redis Hash 字段数量
     * @param key Redis 键
     * @return 字段数量，异常返回 null
     */
    public static Long getHashSize(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("getHashSize skipped: key is blank");
            return null;
        }
        try {
            Long size = redisTemplate.opsForHash().size(key);
            log.info("Hash size - key: {}, size: {}", key, size);
            return size;
        } catch (Exception e) {
            log.error("Error getting hash size - key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取 Redis Hash 中指定字段的值
     * @param key Redis 键
     * @param item Hash 字段名
     * @return 字段值，异常或无效参数返回 null
     */
    public static Object hget(String key, String item) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(item)) {
            log.warn("hget skipped: key or item is blank");
            return null;
        }
        try {
            Object value = redisTemplate.opsForHash().get(key, item);
            log.info("HGET - key: {}, item: {}, value: {}", key, item, value);
            return value;
        } catch (Exception e) {
            log.error("HGET error - key: {}, item: {}", key, item, e);
            return null;
        }
    }

    /**
     * 尝试获取分布式锁（使用 String Redis 键的 setIfAbsent 方式）
     * @param lockKey 锁的键
     * @param timeout 锁有效时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     */
    public static boolean tryLock(String lockKey, long timeout, TimeUnit unit) {
        if (StringUtils.isBlank(lockKey) || timeout <= 0) {
            log.warn("tryLock skipped: lockKey blank or timeout invalid");
            return false;
        }
        Boolean success = valueRedis.setIfAbsent(lockKey, "locked", timeout, unit);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁，只有当前锁值为 "locked" 时才删除
     * @param lockKey 锁键
     * @return 是否成功释放
     */
    public static boolean releaseLock(String lockKey) {
        if (StringUtils.isBlank(lockKey)) {
            log.warn("releaseLock skipped: lockKey is blank");
            return false;
        }
        if (Objects.equals(valueRedis.get(lockKey), "locked")) {
            redisTemplate.delete(lockKey);
            return true;
        }
        return false;
    }

    /**
     * 扫描匹配的 Redis Hash 键，将其转换为指定类型列表
     * @param keyPattern Redis Key 模式，如 "order:*"
     * @param scanCount 每次扫描的条数
     * @param targetType 目标转换类型
     * @param <T> 泛型类型
     * @return 转换后的列表，异常时返回空列表
     */
    public static <T> List<T> scanRedisHashData(String keyPattern, int scanCount, Class<T> targetType) {
        List<T> result = new ArrayList<>();
        RedisConnection connection = null;
        Cursor<byte[]> cursor = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            connection = redisTemplate.getConnectionFactory().getConnection();
            cursor = connection.scan(
                    ScanOptions.scanOptions().match(keyPattern).count(scanCount).build()
            );

            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                Map<String, String> map = hashOps.entries(key);

                if (map != null && !map.isEmpty()) {
                    T obj = objectMapper.convertValue(map, targetType);
                    result.add(obj);
                }
            }
        } catch (Exception e) {
            log.error("scanRedisHashData error for pattern {}: {}", keyPattern, e.getMessage());
        } finally {
            try {
                if (cursor != null) cursor.close();
            } catch (Exception e) {
                log.warn("Error closing Redis cursor: {}", e.getMessage());
            }
            try {
                if (connection != null) connection.close();
            } catch (Exception e) {
                log.warn("Error closing Redis connection: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * 批量写入 {@code Map<String, T>} 类型数据到 Redis Hash，并设置过期时间（秒）
     * @param dataMap key {@code ->} 对象映射
     * @param targetType 对象类型
     * @param expireTime 过期时间，秒，可为 null 表示不设置
     * @param <T> 泛型
     */
    public static <T> void batchSaveAsRedisHash(Map<String, T> dataMap, Class<T> targetType, Long expireTime) {
        if (dataMap == null || dataMap.isEmpty()) {
            log.warn("batchSaveAsRedisHash skipped: dataMap is empty");
            return;
        }

        RedisConnection connection = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            connection = redisTemplate.getConnectionFactory().getConnection();
            connection.openPipeline();

            for (Map.Entry<String, T> entry : dataMap.entrySet()) {
                byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                T value = entry.getValue();

                Map<String, String> valueMap = objectMapper.convertValue(
                        value,
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)
                );

                for (Map.Entry<String, String> fieldEntry : valueMap.entrySet()) {
                    connection.hSet(
                            keyBytes,
                            fieldEntry.getKey().getBytes(StandardCharsets.UTF_8),
                            fieldEntry.getValue().getBytes(StandardCharsets.UTF_8)
                    );
                }

                if (expireTime != null && expireTime > 0) {
                    connection.expire(keyBytes, expireTime);
                }
            }
            connection.closePipeline();

        } catch (Exception e) {
            log.error("batchSaveAsRedisHash error: {}", e.getMessage(), e);
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (Exception e) {
                log.warn("Error closing Redis connection: {}", e.getMessage());
            }
        }
    }
}
