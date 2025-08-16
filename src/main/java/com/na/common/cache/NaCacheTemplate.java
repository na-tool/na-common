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

/**
 * Redis缓存操作工具类
 * 提供全面的Redis操作封装，包括字符串、哈希、列表、集合、有序集合等数据结构的操作
 * 支持分布式锁、缓存过期管理和批量操作
 */
@Slf4j
public class NaCacheTemplate {

    /**
     * Redis模板实例，通过Spring上下文获取
     */
    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis字符串操作对象
     */
    private static ValueOperations<String, Object> valueRedis;

    static {
        redisTemplate = NaSpringContextUtil.getBean("redisTemplate", RedisTemplate.class);
        valueRedis = redisTemplate.opsForValue();
    }

    /**
     * 删除指定缓存键
     * @param key 缓存键
     *
     */
    public static void clear(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("clear skipped: key is blank");
            return;
        }
        redisTemplate.delete(key);
    }

    /**
     * 删除所有以指定前缀开头的缓存键
     * @param prefix 键前缀
     *
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
     * @param pattern 键中包含的字符串
     *
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
     * 获取所有以指定前缀开头的缓存键对应的值
     * @param prefix 键前缀
     * @return 值集合
     */
    public static List<Object> getValuesByPrefix(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            log.warn("getValuesByPrefix skipped: prefix is blank");
            return Collections.emptyList();
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 获取所有以指定前缀开头的缓存键对应的值
     * @param prefix 键前缀
     * @return 键值集合
     */
    public static Map<String, Object> getKeyValueByPrefix(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            log.warn("getKeyValueByPrefix skipped: prefix is blank");
            return Collections.emptyMap();
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, Object> result = new HashMap<>();
        int i = 0;
        for (String key : keys) {
            result.put(key, values.get(i++));
        }
        return result;
    }

    /**
     * 获取所有以指定前缀开头的缓存键
     * @param prefix 键前缀
     * @return 键集合
     */
    public static Set<String> getKeysByPrefix(String prefix,Integer count) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptySet();
        }
        if(count == null){
            count =100;
        }

        Set<String> keys = new HashSet<>();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection()
                .scan(ScanOptions.scanOptions().match(prefix + "*").count(count).build())) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("扫描 Redis keys 出错, prefix={}", prefix, e);
        }
        return keys;
    }



    /**
     * 存储对象为JSON字符串到Redis缓存（无过期时间）
     * @param key 缓存键
     * @param obj 缓存对象
     * @param <T> 对象类型
     *
     */
    public static <T> void setCache(String key, T obj) {
        if (StringUtils.isBlank(key)) {
            log.warn("setCache skipped: key is blank");
            return;
        }
        valueRedis.set(key, JSONObject.toJSONString(obj));
    }

    /**
     * 存储对象为JSON字符串到Redis缓存，并设置过期时间
     * @param key 缓存键
     * @param obj 缓存对象
     * @param time 过期时间
     * @param timeUnit 时间单位
     * @param <T> 对象类型
     *
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
     * @return JSON字符串，找不到返回null
     *
     */
    public static String getCache(String key) {
        if (StringUtils.isBlank(key)) {
            log.warn("getCache skipped: key is blank");
            return null;
        }
        return String.valueOf(valueRedis.get(key));
    }

    /**
     * 获取指定类型的缓存对象
     * @param key 缓存键
     * @param tClass 目标类型Class
     * @param <T> 泛型类型
     * @return 目标类型对象，找不到或解析失败返回null
     *
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
     * @param tClass 目标类型Class
     * @param function 缓存未命中时调用的函数
     * @param <T> 泛型类型
     * @return 缓存数据或回调结果
     *
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
     *
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
     * @param clazz 元素类型Class
     * @param <T> 原始缓存类型
     * @return Set，找不到或解析失败返回null
     *
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
     * 获取指定类型数据，如果没有则执行function，结果转成{@code List<B> }
     * @param key 缓存键
     * @param tClass 缓存数据的原始类型（通常为List.class）
     * @param function 缓存未命中时回调函数
     * @param itemClass 最终列表元素类型
     * @param <T> 缓存数据类型泛型
     * @param <B> 最终元素类型泛型
     * @return {@code List<B> }转换后的列表
     *
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
     * 获取指定类型数据，如果没有则执行function，结果转成{@code Set<B> }
     * @param key 缓存键
     * @param tClass 缓存数据的原始类型
     * @param function 缓存未命中时回调函数
     * @param itemClass 最终元素类型
     * @param <T> 缓存数据泛型
     * @param <B> 最终元素泛型
     * @return {@code Set<B> }转换后的集合
     *
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
     * 获取指定类型数据，如果没有则执行function，结果转成{@code Deque<B>}
     * @param key 缓存键
     * @param tClass 缓存数据的原始类型
     * @param function 缓存未命中时回调函数
     * @param itemClass 元素类型
     * @param <T> 缓存数据泛型
     * @param <B> 元素泛型
     * @return {@code Deque<B>}转换后的双端队列
     *
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
     *
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
     * @return true存在，false不存在或key为空
     *
     */
    public static boolean exists(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Redis Hash存值
     * @param key Redis键
     * @param item Hash字段名
     * @param value 字段值
     * @return 操作是否成功
     *
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
     * Redis Hash存值并设置过期时间（秒）
     * @param key Redis键
     * @param item Hash字段名
     * @param value 字段值
     * @param time 过期时间，秒，{@code > }0才生效
     * @return 操作是否成功
     *
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
     * @param time 过期时间秒数，必须 {@code >}0
     * @return 是否成功
     *
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
     * 获取Redis Hash字段数量
     * @param key Redis键
     * @return 字段数量，异常返回null
     *
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
     * 获取Redis Hash中指定字段的值
     * @param key Redis键
     * @param item Hash字段名
     * @return 字段值，异常或无效参数返回null
     *
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
     * 尝试获取分布式锁（使用String Redis键的setIfAbsent方式）
     * @param lockKey 锁的键
     * @param timeout 锁有效时间
     * @param unit 时间单位
     * @return 是否成功获取锁
     *
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
     * 释放锁，只有当前锁值为"locked"时才删除
     * @param lockKey 锁键
     * @return 是否成功释放
     *
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
     * 扫描匹配的Redis Hash键，将其转换为指定类型列表
     * @param keyPattern Redis Key模式，如"order:*"
     * @param scanCount 每次扫描的条数
     * @param targetType 目标转换类型
     * @param <T> 泛型类型
     * @return 转换后的列表，异常时返回空列表
     *
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
     * 批量写入{@code Map<String, T>}类型数据到Redis Hash，并设置过期时间（秒）
     * @param dataMap key {@code ->} 对象映射
     * @param targetType 对象类型
     * @param expireTime 过期时间，秒，可为null表示不设置
     * @param <T> 泛型
     *
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

    /**
     * 递增操作
     * @param key 键
     * @param delta 要增加的值(必须大于0)
     * @return 递增后的值
     *
     */
    public Long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减操作
     * @param key 键
     * @param delta 要减少的值(必须大于0)
     * @return 递减后的值
     *
     */
    public Long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     *
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 批量存储Hash字段
     * @param key 键
     * @param map 对应多个键值
     * @return true成功 false失败
     *
     */
    public boolean hmSet(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 批量存储Hash字段并设置时间
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     *
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除Hash表中的一个字段
     * @param key 键 不能为null
     * @param hashKey 项 不能为null
     *
     */
    public void hmDelete(String key, Object hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    /**
     * 删除Hash表中的多个字段
     * @param key 键 不能为null
     * @param item 项 可以是多个 不能为null
     *
     */
    public void hmDelete(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断Hash表中是否有该项的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true存在 false不存在
     *
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * Hash字段递增
     * @param key 键
     * @param item 项
     * @param by 要增加几(大于0)
     * @return 递增后的值
     *
     */
    public long hincr(String key, String item, long by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * Hash字段递减
     * @param key 键
     * @param item 项
     * @param by 要减少几(大于0)
     * @return 递减后的值
     *
     */
    public long hdecr(String key, String item, long by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    /**
     * 向列表左侧添加元素
     * @param key 键
     * @param value 值
     * @return 操作是否成功
     *
     */
    public boolean lPush(String key, Object value) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向列表左侧添加元素并设置过期时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return 操作是否成功
     *
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            if (time > 0)
                expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从列表右侧弹出一个元素（阻塞式）
     * @param k 键
     * @param t 超时秒数
     * @return 弹出的元素，如果超时返回null
     *
     */
    public Object getRightPop(String k, Long t) {
        return redisTemplate.opsForList().rightPop(k, t, TimeUnit.SECONDS);
    }

    /**
     * 获取列表长度
     * @param key 键
     * @return 列表长度
     *
     */
    public Long getListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 获取列表指定范围的元素
     * @param key 键
     * @param start 开始索引
     * @param end 结束索引，0到-1代表所有元素
     * @return 列表元素
     *
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过索引获取列表中的元素
     * @param key 键
     * @param index 索引，index {@code >=0 }时从头部开始，index{@code <}0时从尾部开始
     * @return 列表元素
     *
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据索引修改列表中的元素
     * @param key 键
     * @param index 索引
     * @param value 值
     * @return 操作是否成功
     *
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除列表中指定值的元素
     * @param key 键
     * @param count 移除数量，正数从头部开始，负数从尾部开始，0移除所有
     * @param value 值
     * @return 移除的个数
     *
     */
    public Long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 根据key获取Set中的所有值
     * @param key 键
     * @return 集合中的所有值
     *
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断集合中是否存在指定值
     * @param key 键
     * @param value 值
     * @return true存在 false不存在
     *
     */
    public Boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向集合中添加元素
     * @param key 键
     * @param values 值，可以是多个
     * @return 成功添加的个数
     *
     */
    public Long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 向集合中添加元素并设置过期时间
     * @param key 键
     * @param time 时间(秒)
     * @param values 值，可以是多个
     * @return 成功添加的个数
     *
     */
    public Long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0)
                expire(key, time);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 获取集合的大小
     * @param key 键
     * @return 集合的大小
     *
     */
    public Long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 从集合中移除指定元素
     * @param key 键
     * @param values 值，可以是多个
     * @return 移除的个数
     *
     */
    public Long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 向有序集合添加元素（适用于排行榜）
     * @param key 键
     * @param value 值
     * @param score 分数，用于排序
     *
     */
    public void zAdd(String key, Object value, double score) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.add(key, value, score);
    }

    /**
     * 获取有序集合中指定分数范围的元素（适用于排行榜）
     * @param key 键
     * @param score 最小分数
     * @param score1 最大分数
     * @return 符合条件的元素集合
     *
     */
    public Set<Object> rangeByScore(String key, double score, double score1) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.rangeByScore(key, score, score1);
    }
}
