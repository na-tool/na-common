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
     * Searcher 的延迟初始化持有者。
     * <p>
     * 用"初始化即按需"惯用法替代静态块：静态块一旦抛异常，JVM 会把本类标记为不可用，
     * 之后任何访问都变成没有原始上下文的 {@code NoClassDefFoundError} —— 把"数据文件缺失"
     * 这个本来可定位、可修复的问题伪装成"类加载失败"。放进持有者后：
     * <ul>
     *   <li>只有真正调用解析方法时才会触发加载，仅仅被类扫描碰到不会让应用起不来</li>
     *   <li>异常信息与 cause 保持完整，直接指明缺失的是哪个 classpath 资源</li>
     * </ul>
     */
    private static final class SearcherHolder {

        /**
         * 将整个 xdb 文件(约 11M)一次性加载到内存。
         * 此种创建方式支持多线程并发调用，全局只需初始化一次。
         * 注意：该 Searcher 为进程级共享单例，严禁调用 close()，
         * 否则内部缓冲区被释放后，后续所有解析请求都会失败。
         */
        private static final Searcher INSTANCE = load();

        private SearcherHolder() {
        }

        /**
         * 加载 xdb 并构造 Searcher
         *
         * @return 进程级共享的 Searcher
         * @throws IllegalStateException 文件缺失或读取失败，消息中给出精确原因
         */
        private static Searcher load() {
            ClassPathResource resource = new ClassPathResource(XDB_PATH);
            if (!resource.exists()) {
                throw new IllegalStateException(
                        "IP 归属地数据文件缺失: classpath:" + XDB_PATH
                                + "；请确认 src/main/resources/" + XDB_PATH + " 已随包发布");
            }
            try (InputStream inputStream = resource.getInputStream()) {
                // getInputStream() 同时兼容文件系统(classpath 目录)和 JAR 包两种部署形态，无需按协议区分读取方式
                Searcher searcher = Searcher.newWithBuffer(inputStream.readAllBytes());
                log.info("ip2region.xdb 加载完成, Searcher 初始化成功");
                return searcher;
            } catch (IOException e) {
                throw new IllegalStateException("读取 IP 归属地数据文件失败: classpath:" + XDB_PATH, e);
            }
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
            log.error("ip 解析为 long 失败, ipStr:[{}], 错误信息:[{}]", ipStr, e.getMessage(), e);
            // 消息带上入参与原因, 便于区分"IP 格式非法"与"数据文件不可用"
            throw new RuntimeException("IP 地址解析失败: " + ipStr, e);
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
            log.error("根据 ip 解析地址失败, ip 为空, index:[{}]", Arrays.toString(index));
            throw new RuntimeException("IP 归属地查询失败: 入参 ip 为 null");
        }
        try {
            // region 格式固定为: 国家|区域|省份|城市|ISP
            String region = SearcherHolder.INSTANCE.search(ip);
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
            log.error("根据 ip 解析地址失败, ip:[{}], index:[{}], 报错信息:[{}]", ip, Arrays.toString(index), e.getMessage(), e);
            throw new RuntimeException("IP 归属地查询失败: " + ip, e);
        }
    }
}
