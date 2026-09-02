package com.na.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.common.utils.dto.RegionDto;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区域工具类
 * 提供区域数据的加载、缓存和查询功能，支持通过区域编码获取区域信息、完整路径等操作
 * 区域数据从类路径下的address.json文件加载，包含省、市、区、街道等多级区域信息
 * <p>
 * 数据在类加载时一次性构建为编码 -> 节点的映射表，之后仅做只读查询，
 * HashMap 初始化完成后不再变更，天然支持多线程并发调用
 */
public class RegionUtil {

    /** 区域数据文件在 classpath 中的路径 */
    private static final String RESOURCE_PATH = "/address.json";

    /** 区域编码与区域节点的映射表，静态初始化后只读，禁止外部修改 */
    private static final Map<String, RegionDto> codeMap;

    static {
        try (InputStream is = RegionUtil.class.getResourceAsStream(RESOURCE_PATH)) {
            // try-with-resources 确保流关闭; getResourceAsStream 同时兼容文件系统与 JAR 包两种部署形态
            if (is == null) {
                // 文件缺失时给出明确错误，避免 NPE 难以排查
                throw new IllegalStateException("未找到区域数据文件:" + RESOURCE_PATH);
            }
            ObjectMapper mapper = new ObjectMapper();
            // 反序列化为多级区域树(省 -> 市 -> 区 -> 街道)
            List<RegionDto> provinces = mapper.readValue(is, new TypeReference<List<RegionDto>>() {});

            // 递归构建区域编码映射表，并设置父节点编码
            Map<String, RegionDto> map = new HashMap<>();
            buildMap(provinces, null, map);
            codeMap = Collections.unmodifiableMap(map);
        } catch (Exception e) {
            throw new RuntimeException("加载区域数据失败", e);
        }
    }

    /** 纯静态工具类，禁止实例化 */
    private RegionUtil() {
    }

    /**
     * 递归构建区域编码映射表
     * @param list 当前层级的区域列表
     * @param parentCode 父级区域编码，顶级区域为null
     * @param map 目标映射表
     */
    private static void buildMap(List<RegionDto> list, String parentCode, Map<String, RegionDto> map) {
        if (list == null) return;
        for (RegionDto region : list) {
            // 反向挂载父编码，供 getFullPath 逆向追溯
            region.parentCode = parentCode;
            map.put(region.code, region);
            // 递归处理子区域
            buildMap(region.children, region.code, map);
        }
    }

    /**
     * 兼容查询，根据区域编码获取区域节点
     * 支持6位省级编码(如"110000")自动转换为2位编码(如"11")进行查询
     * @param code 区域编码，支持不同层级的编码格式
     * @return 对应的区域节点对象，若未找到则返回null
     */
    public static RegionDto getRegionByCode(String code) {
        if (code == null) return null;
        // 兼容6位省级编码: 先按完整编码查，查不到再降级用前2位查
        if (code.length() == 6 && code.endsWith("0000")) {
            RegionDto region = codeMap.get(code);
            return region != null ? region : codeMap.get(code.substring(0, 2));
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
        RegionDto current = getRegionByCode(code);
        if (current == null) return new ArrayList<>();

        // 先正向收集(当前 -> 顶级)，再整体反转，避免 LinkedList 头插的额外开销
        List<String> path = new ArrayList<>();
        while (current != null) {
            path.add(current.name);
            current = codeMap.get(current.parentCode);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * 根据区域编码获取对应的区域名称
     * @param code 区域编码
     * @return 区域名称，若编码不存在则返回null
     */
    public static String getNameByCode(String code) {
        RegionDto region = getRegionByCode(code);
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
