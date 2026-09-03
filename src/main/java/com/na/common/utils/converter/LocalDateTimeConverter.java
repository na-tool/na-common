package com.na.common.utils.converter;

import org.apache.commons.beanutils.Converter;

import java.time.LocalDateTime;

public class LocalDateTimeConverter implements Converter {
    @Override
    public <T> T convert(Class<T> type, Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) {
            return type.cast(value);
        }
        return type.cast(LocalDateTime.parse(value.toString()));
    }
}

