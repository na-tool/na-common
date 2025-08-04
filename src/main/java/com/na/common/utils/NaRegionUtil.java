package com.na.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.common.utils.dto.NaRegionDto;

import java.io.InputStream;
import java.util.*;

public class NaRegionUtil {
    // code → 区域节点
    private static final Map<String, NaRegionDto> codeMap = new HashMap<>();

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = NaRegionUtil.class.getResourceAsStream("/address.json");
            List<NaRegionDto> provinces = mapper.readValue(is, new TypeReference<List<NaRegionDto>>() {});

            // 递归构建 codeMap，并设置 parentCode
            buildMap(provinces, null);
        } catch (Exception e) {
            throw new RuntimeException("加载区域数据失败", e);
        }
    }

    private static void buildMap(List<NaRegionDto> list, String parentCode) {
        if (list == null) return;
        for (NaRegionDto region : list) {
            region.parentCode = parentCode;
            codeMap.put(region.code, region);
            buildMap(region.children, region.code);
        }
    }

    /**
     * 兼容查询，根据 code 获取区域节点
     * 支持6位省级code如 "110000" 转换为 2位 "11"
     */
    public static NaRegionDto getRegionByCode(String code) {
        if (code == null) return null;
        if (code.length() == 6 && code.endsWith("0000")) {
            // 先尝试完整6位查找
            NaRegionDto region = codeMap.get(code);
            if (region != null) return region;
            // 不存在则尝试省级2位code查找
            String shortCode = code.substring(0, 2);
            return codeMap.get(shortCode);
        }
        return codeMap.get(code);
    }

    /**
     * 根据 code 获取完整路径，如：[北京市, 市辖区, 东城区, 东华门街道]
     */
    public static List<String> getFullPath(String code) {
        List<String> path = new LinkedList<>();
        NaRegionDto current = getRegionByCode(code);
        if (current == null) return path;

        // 逆向找父节点
        while (current != null) {
            path.add(0, current.name);
            current = codeMap.get(current.parentCode);
        }

        return path;
    }

    /**
     * 根据 code 获取单个名称
     */
    public static String getNameByCode(String code) {
        NaRegionDto region = getRegionByCode(code);
        return region != null ? region.name : null;
    }

    public static void main(String[] args) {
        System.out.println(getFullPath("110101001"));
        // 输出: [北京市, 市辖区, 东城区, 东华门街道]

        System.out.println(getFullPath("110101"));
        // 输出: [北京市, 市辖区, 东城区]

        System.out.println(getFullPath("11"));
        // 输出: [北京市]

        System.out.println(getFullPath("110000"));
        // 输出: [北京市]
    }
}
