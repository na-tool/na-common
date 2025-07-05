package com.na.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.common.utils.converter.LocalDateConverter;
import com.na.common.utils.converter.LocalDateTimeConverter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NaObjConversionUtil {

    private static final BeanUtilsBean beanUtils;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        ConvertUtilsBean convertUtils = new ConvertUtilsBean();
        // 注册 LocalDateTime 转换器
        convertUtils.register(new LocalDateTimeConverter(), LocalDateTime.class);

        // 注册 LocalDate 转换器
        convertUtils.register(new LocalDateConverter(), LocalDate.class);

        // 注册 BigDecimal，Long，Boolean 等基础类型
        convertUtils.register(new BigDecimalConverter(null), BigDecimal.class);
        convertUtils.register(new LongConverter(null), Long.class);
        convertUtils.register(new BooleanConverter(null), Boolean.class);
        convertUtils.register(new IntegerConverter(null), Integer.class);
        convertUtils.register(new DoubleConverter(null), Double.class);
        convertUtils.register(new FloatConverter(null), Float.class);

        // 注册到 BeanUtilsBean 实例中
        beanUtils = new BeanUtilsBean(convertUtils);
    }

    public static void copyProperties(Object source, Object target) {
        try {
            beanUtils.copyProperties(target, source);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy properties from " +
                    source.getClass().getSimpleName() + " to " + target.getClass().getSimpleName(), e);
        }
    }

    public static <T> T copyProperties(Object source, Class<T> targetType) {
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            beanUtils.copyProperties(target, source);
            return target;
        } catch (Exception e) {
            System.out.println("Failed to instantiate and copy to " +
                    targetType.getSimpleName() + e);
        }
        return null;
    }

    public static <T> List<T> copyPropertiesList(List<?> sourceList, Class<T> targetType) {
        List<T> targetList = new ArrayList<>();
        for (Object source : sourceList) {
            T t = copyProperties(source, targetType);
            if( t != null){
                targetList.add(t);
            }

        }
        return targetList;
    }

    /**
     * 复制 source 中非 null 且非空字符串的字段到 target
     *
     * @param source 源对象
     * @param target 目标对象
     * @param clazz 目标对象的类类型
     * @param <T> 类型参数
     */
    public static <T> void copyNonNullAndNonEmptyFields(T source, T target, Class<T> clazz) {
        if (source == null || target == null) return;

        Map<String, Object> map = objectMapper.convertValue(source, Map.class);

        // 移除 null 和空字符串字段
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
     * 将 target 中非 null 且非空字符串的字段，赋值给 source 中对应字段，包括继承字段
     *
     * @param <T> 类型参数，表示对象的类型
     * @param source 源对象，包含需要复制的字段
     * @param target 目标对象，字段会被复制到此对象
     */
    public static <T> void overwriteNonNullAndNonEmptyFields(T source, T target) {
        if (source == null || target == null) return;

        List<Field> fields = getAllFields(source.getClass());

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object targetVal = field.get(target);
                if (targetVal != null) {
                    if (targetVal instanceof String && ((String) targetVal).trim().isEmpty()) {
                        continue;
                    }
                    field.set(source, targetVal);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to overwrite field: " + field.getName(), e);
            }
        }
    }

    /**
     * 获取类及其父类的所有字段
     */
    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Field[] declared = c.getDeclaredFields();
            for (Field f : declared) {
                fields.add(f);
            }
        }
        return fields;
    }
}
