package com.na.common.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.na.common.utils.converter.LocalDateConverter;
import com.na.common.utils.converter.LocalDateTimeConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对象转换工具类
 * <p>
 * 基于 commons-beanutils(属性复制+类型转换) 与 Jackson(map过滤) 实现的对象/字段拷贝能力：
 * <ul>
 *   <li>{@link #copyProperties(Object, Object)}: 常规属性复制，null 字段也会覆盖</li>
 *   <li>{@link #copyNonNullAndNonEmptyFields(Object, Object, Class)}: 仅复制非 null 且非空串的字段</li>
 *   <li>{@link #overwriteNonNullAndNonEmptyFields(Object, Object)}: 反射方式按字段(含继承)覆盖，跳过静态与基本类型字段</li>
 * </ul>
 * 注意: beanutils 转换器已注册 BigDecimal/Long/Boolean 等包装类型(转换失败置 null 而非抛错)，
 * 以及自定义的 LocalDateTime/LocalDate 转换器
 */
@Slf4j
public final class ObjConversionUtil {

    private static final BeanUtilsBean beanUtils;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        ConvertUtilsBean convertUtils = new ConvertUtilsBean();
        // 注册 LocalDateTime / LocalDate 自定义转换器(项目 converter 包内实现)
        convertUtils.register(new LocalDateTimeConverter(), LocalDateTime.class);
        convertUtils.register(new LocalDateConverter(), LocalDate.class);

        // 注册 BigDecimal、Long 等包装类型转换器
        // 构造参数传 null: 源值为 null/空/非法时不抛异常，转换结果为 null(默认行为是转成 0，会污染数据)
        convertUtils.register(new BigDecimalConverter(null), BigDecimal.class);
        convertUtils.register(new LongConverter(null), Long.class);
        convertUtils.register(new BooleanConverter(null), Boolean.class);
        convertUtils.register(new IntegerConverter(null), Integer.class);
        convertUtils.register(new DoubleConverter(null), Double.class);
        convertUtils.register(new FloatConverter(null), Float.class);

        beanUtils = new BeanUtilsBean(convertUtils);

        // 注册 java.time 支持: 项目未引入 jackson-datatype-jsr310，不注册时
        // convertValue 遇到 LocalDateTime/LocalDate 字段会抛 InvalidDefinitionException
        objectMapper.registerModule(buildJavaTimeModule());
    }

    /** 纯静态工具类，禁止实例化 */
    private ObjConversionUtil() {
    }

    /**
     * 构建 java.time 类型(ISO-8601 字符串格式)的 Jackson 序列化模块
     */
    private static Module buildJavaTimeModule() {
        return new SimpleModule("JavaTimeModule")
                .addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                    @Override
                    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider sp) throws IOException {
                        // ISO-8601: yyyy-MM-dd'T'HH:mm:ss
                        gen.writeString(value.toString());
                    }
                })
                .addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                    @Override
                    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        return LocalDateTime.parse(p.getValueAsString());
                    }
                })
                .addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
                    @Override
                    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider sp) throws IOException {
                        // ISO-8601: yyyy-MM-dd
                        gen.writeString(value.toString());
                    }
                })
                .addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
                    @Override
                    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        return LocalDate.parse(p.getValueAsString());
                    }
                });
    }

    /**
     * 复制 source 的同名属性到 target（null 值也会覆盖）
     * <p>
     * 需要跳过 null/空字段时请使用 {@link #copyNonNullAndNonEmptyFields(Object, Object, Class)}
     *
     * @param source 源对象
     * @param target 目标对象，同名属性被覆盖
     * @throws RuntimeException 复制失败时抛出，原始异常作为 cause 保留
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            // 原实现在此场景会于 catch 中因 source.getClass() 二次 NPE，这里显式给出清晰错误
            throw new IllegalArgumentException("copyProperties 的 source 与 target 均不能为 null");
        }
        try {
            beanUtils.copyProperties(target, source);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy properties from " +
                    source.getClass().getSimpleName() + " to " + target.getClass().getSimpleName(), e);
        }
    }

    /**
     * 实例化 target 类型并复制 source 的同名属性
     *
     * @param source     源对象
     * @param targetType 目标类型，需提供无参构造器
     * @param <T>        目标类型
     * @return 复制后的新对象；source/targetType 为 null 或实例化、复制失败时返回 null
     */
    public static <T> T copyProperties(Object source, Class<T> targetType) {
        if (source == null || targetType == null) {
            return null;
        }
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            beanUtils.copyProperties(target, source);
            return target;
        } catch (Exception e) {
            // 保持返回 null 的语义(批量复制场景依赖 null 跳过失败项)，仅修复输出方式
            log.error("Failed to instantiate and copy to {}", targetType.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 批量复制对象列表
     *
     * @param sourceList 源对象列表
     * @param targetType 目标类型，需提供无参构造器
     * @param <T>        目标类型
     * @return 目标列表；入参为 null/空时返回空列表(不返回 null)；复制失败的单个元素会被跳过
     */
    public static <T> List<T> copyPropertiesList(List<?> sourceList, Class<T> targetType) {
        if (sourceList == null || sourceList.isEmpty() || targetType == null) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (Object source : sourceList) {
            T t = copyProperties(source, targetType);
            if (t != null) {
                targetList.add(t);
            }
        }
        return targetList;
    }

    /**
     * 仅复制 source 中非 null 且非空字符串(含纯空白)的字段到 target，其余字段保留 target 原值
     * <p>
     * 实现方式: Jackson 序列化为 Map → 过滤空值 → 反序列化为同类型对象 → beanutils 复制。
     * 字段名以 getter/setter 属性名为准；静态字段与 @JsonIgnore 标注的字段不参与
     *
     * @param source 源对象
     * @param target 目标对象
     * @param clazz  source 的类类型
     * @param <T>    类型参数
     * @throws RuntimeException 过滤或复制失败时抛出
     */
    public static <T> void copyNonNullAndNonEmptyFields(T source, T target, Class<T> clazz) {
        if (source == null || target == null || clazz == null) {
            return;
        }

        // 序列化为属性 Map(指定泛型避免原生 Map 的 unchecked 警告)
        Map<String, Object> map = objectMapper.convertValue(source, new TypeReference<Map<String, Object>>() {
        });

        // 移除 null 与空字符串(含纯空白)字段
        map = map.entrySet().stream()
                .filter(e -> {
                    Object val = e.getValue();
                    return val != null && (!(val instanceof String) || !((String) val).trim().isEmpty());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        T filtered = objectMapper.convertValue(map, clazz);

        try {
            beanUtils.copyProperties(target, filtered);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to copy non-null and non-empty fields", e);
        }
    }

    /**
     * 将 target 中非 null 且非空字符串的字段覆盖到 source 的同名字段(含父类继承字段)。
     * 注意参数方向: source 是"被更新"的对象
     * <p>
     * 反射直接按字段操作(无视 getter/setter)，跳过 static 字段(如 serialVersionUID，写入会抛异常)
     * 与基本类型字段(基本类型永不为 null，其默认值 0/false 会误覆盖真实数据)及合成字段
     *
     * @param source 被更新的对象
     * @param target 数据来源对象
     * @param <T>    类型参数
     * @throws RuntimeException 字段写入失败时抛出
     */
    public static <T> void overwriteNonNullAndNonEmptyFields(T source, T target) {
        if (source == null || target == null) {
            return;
        }

        for (Field field : getAllFields(source.getClass())) {
            // 跳过静态字段: 对 static final 字段(如 serialVersionUID)调用 set 会直接抛 IllegalAccessException
            // 跳过基本类型: 其默认值(0/false)被包装为非 null，无法区分"未赋值"与"真实值"，会误覆盖
            // 跳过合成字段: 编译器生成的 this$0/$jacocoData 等
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || field.getType().isPrimitive() || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object targetVal = field.get(target);
                if (targetVal == null) {
                    continue;
                }
                // 空字符串(含纯空白)视为未赋值，跳过
                if (targetVal instanceof String && ((String) targetVal).trim().isEmpty()) {
                    continue;
                }
                field.set(source, targetVal);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to overwrite field: " + field.getName(), e);
            }
        }
    }

    /**
     * 获取类及其所有父类(至 Object 为止)声明的字段，子类字段在前
     */
    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }
}
