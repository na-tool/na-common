package com.na.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.common.utils.dto.NaRegionDto;

import java.io.InputStream;
import java.util.*;

/**
 * 区域工具类
 * 提供区域数据的加载、缓存和查询功能，支持通过区域编码获取区域信息、完整路径等操作
 * 区域数据从类路径下的address.json文件加载，包含省、市、区、街道等多级区域信息
 */
public class NaRegionUtil {
    // 区域编码与区域节点的映射表，用于快速查询
    private static final Map<String, NaRegionDto> codeMap = new HashMap<>();

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 从类路径加载区域数据JSON文件
            InputStream is = NaRegionUtil.class.getResourceAsStream("/address.json");
            List<NaRegionDto> provinces = mapper.readValue(is, new TypeReference<List<NaRegionDto>>() {});

            // 递归构建区域编码映射表，并设置父节点编码
            buildMap(provinces, null);
        } catch (Exception e) {
            throw new RuntimeException("加载区域数据失败", e);
        }
    }

    /**
     * 递归构建区域编码映射表
     * @param list 当前层级的区域列表
     * @param parentCode 父级区域编码，顶级区域为null
     */
    private static void buildMap(List<NaRegionDto> list, String parentCode) {
        if (list == null) return;
        for (NaRegionDto region : list) {
            region.parentCode = parentCode;
            codeMap.put(region.code, region);
            // 递归处理子区域
            buildMap(region.children, region.code);
        }
    }

    /**
     * 兼容查询，根据区域编码获取区域节点
     * 支持6位省级编码(如"110000")自动转换为2位编码(如"11")进行查询
     * @param code 区域编码，支持不同层级的编码格式
     * @return 对应的区域节点对象，若未找到则返回null
     */
    public static NaRegionDto getRegionByCode(String code) {
        if (code == null) return null;
        // 处理6位省级编码的兼容情况
        if (code.length() == 6 && code.endsWith("0000")) {
            // 先尝试完整6位编码查找
            NaRegionDto region = codeMap.get(code);
            if (region != null) return region;
            // 若未找到，则尝试使用2位省级编码查找
            String shortCode = code.substring(0, 2);
            return codeMap.get(shortCode);
        }
        return codeMap.get(code);
    }

    /**
     * 根据区域编码获取完整的区域路径
     * 路径从顶级区域(省)开始，到当前区域结束
     * @param code 区域编码
     * @return 区域路径列表，按层级从高到低排列，如[北京市, 市辖区, 东城区, 东华门街道]
     *         若编码不存在，则返回空列表
     */
    public static List<String> getFullPath(String code) {
        List<String> path = new LinkedList<>();
        NaRegionDto current = getRegionByCode(code);
        if (current == null) return path;

        // 逆向追溯父节点，构建完整路径
        while (current != null) {
            path.add(0, current.name);
            current = codeMap.get(current.parentCode);
        }

        return path;
    }

    /**
     * 根据区域编码获取对应的区域名称
     * @param code 区域编码
     * @return 区域名称，若编码不存在则返回null
     */
    public static String getNameByCode(String code) {
        NaRegionDto region = getRegionByCode(code);
        return region != null ? region.name : null;
    }

    /**
     * 主方法用于测试区域工具类的功能
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 测试街道级编码
        System.out.println(getFullPath("110101001"));
        // 输出: [北京市, 市辖区, 东城区, 东华门街道]

        // 测试区级编码
        System.out.println(getFullPath("110101"));
        // 输出: [北京市, 市辖区, 东城区]

        // 测试省级2位编码
        System.out.println(getFullPath("11"));
        // 输出: [北京市]

        // 测试省级6位编码(兼容处理)
        System.out.println(getFullPath("110000"));
        // 输出: [北京市]
    }
}
