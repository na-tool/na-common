package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IP 归属地解析工具类
 * <p>
 * 基于 ip2region 的 xdb 数据文件实现，region 返回格式固定为：国家|区域|省份|城市|ISP
 */
@Slf4j
public class IpParseUtil {

    /** xdb 数据文件在 classpath 中的路径 */
    private static final String XDB_PATH = "ip2region.xdb";

    /**
     * 将整个 xdb 文件(约 11M)一次性加载到内存。
     * 此种创建方式支持多线程并发调用，全局只需初始化一次。
     * 注意：该 Searcher 为进程级共享单例，严禁调用 close()，
     * 否则内部缓冲区被释放后，后续所有解析请求都会失败。
     */
    private static final Searcher SEARCHER;

    static {
        try (InputStream inputStream = new ClassPathResource(XDB_PATH).getInputStream()) {
            // getInputStream() 同时兼容文件系统(classpath 目录)和 JAR 包两种部署形态，无需按协议区分读取方式
            SEARCHER = Searcher.newWithBuffer(inputStream.readAllBytes());
            log.info("加载了ip2region.xdb文件, Searcher初始化完成!");
        } catch (IOException e) {
            log.error("初始化ip2region.xdb文件失败,报错信息:[{}]", e.getMessage(), e);
            throw new RuntimeException("系统异常!", e);
        }
    }

    /** 纯静态工具类，禁止实例化 */
    private IpParseUtil() {
    }

    /**
     * 解析ip地址(返回全部字段)
     *
     * @param ipStr 字符串类型ip 例:192.168.0.1
     * @return 返回结果形式(国家|区域|省份|城市|ISP) 例 [中国, 0, 河北省, 衡水市, 电信]
     */
    public static List<String> parse(String ipStr) {
        return parse(ipStr, null);
    }

    /**
     * 自定义解析ip地址
     *
     * @param ipStr 字符串类型ip 例:192.168.0.1
     * @param index 想要获取的字段下标(0国家/1区域/2省份/3城市/4ISP) 例如:只想获取 省,市 index = [2,3]
     * @return 返回结果例 [河北省, 衡水市]
     */
    public static List<String> parse(String ipStr, int[] index) {
        try {
            // 先校验并将点分十进制 ip 转为 long，非法格式会在此抛出异常
            long ip = Searcher.checkIP(ipStr);
            return parse(ip, index);
        } catch (Exception e) {
            log.error("ip解析为long错误,ipStr:[{}],错误信息:[{}]", ipStr, e.getMessage(), e);
            throw new RuntimeException("系统异常!", e);
        }
    }

    /**
     * 自定义解析ip地址
     *
     * @param ip    Long类型ip(已经过转换的点分十进制ip)
     * @param index 想要获取的字段下标(0国家/1区域/2省份/3城市/4ISP) 例如:只想获取 省,市 index = [2,3]
     * @return 返回结果例 [河北省, 衡水市]
     */
    public static List<String> parse(Long ip, int[] index) {
        if (ip == null) {
            log.error("根据ip解析地址失败,ip为空,index:[{}]", Arrays.toString(index));
            throw new RuntimeException("系统异常!");
        }
        try {
            // region 格式固定为: 国家|区域|省份|城市|ISP
            String region = SEARCHER.search(ip);
            String[] split = region.split("\\|");
            if (index == null) {
                return Arrays.asList(split);
            }
            List<String> regionList = new ArrayList<>(index.length);
            for (int i : index) {
                // 下标越界(如数据字段数不足)时由 catch 统一记录上下文日志
                regionList.add(split[i]);
            }
            return regionList;
        } catch (Exception e) {
            log.error("根据ip解析地址失败,ip:[{}],index:[{}],报错信息:[{}]", ip, Arrays.toString(index), e.getMessage(), e);
            throw new RuntimeException("系统异常!", e);
        }
    }
}
