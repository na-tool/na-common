package com.na.common.utils.converter;

import org.apache.commons.beanutils.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter implements Converter {
    @Override
    public <T> T convert(Class<T> type, Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) {
            return type.cast(value);
        }
        try {
//            return type.cast(LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE));
            return type.cast(LocalDate.parse(value.toString()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert value to LocalDate: " + value, e);
        }
    }
}
