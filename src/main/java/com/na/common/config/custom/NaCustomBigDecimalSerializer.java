package com.na.common.config.custom;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

public class NaCustomBigDecimalSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
//            // 判断小数点后是否有值，如果没有则返回整数，否则返回正常格式
//            if (value.stripTrailingZeros().scale() <= 0) {
//                gen.writeNumber(String.valueOf(value.intValue()));  // 返回整数部分
//            } else {
//                gen.writeNumber(value.toString());  // 返回原始 BigDecimal 格式
//            }
            gen.writeString(value.stripTrailingZeros().toPlainString());
        }
    }
}

