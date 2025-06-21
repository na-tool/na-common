package com.na.common.utils;

import com.na.common.utils.converter.LocalDateConverter;
import com.na.common.utils.converter.LocalDateTimeConverter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NaObjConversionUtil {

    private static final BeanUtilsBean beanUtils;

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
}
