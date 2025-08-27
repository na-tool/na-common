package com.na.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class NaMapUtil {
    /**
     * 将任意对象转换为 Map<String, Object>
     * 使用 fastjson 保留字段顺序
     *
     * @param obj 任意对象（POJO、VO、Map 等）
     * @return LinkedHashMap，字段顺序与对象一致
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return new LinkedHashMap<>();
        }

        // 1. 转成 JSONObject
        JSONObject jsonObject = (JSONObject) JSON.toJSON(obj);

        // 2. 转成 LinkedHashMap 保留顺序
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : jsonObject.keySet()) {
            map.put(key, jsonObject.get(key));
        }

        return map;
    }
}
