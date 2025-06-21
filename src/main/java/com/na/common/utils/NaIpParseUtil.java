package com.na.common.utils;


import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
public class NaIpParseUtil {

    /**
     * 将整个xdb文件加载到内存中(11M左右),此种创建方式支持多线程,因此只需要加载一次
     */
    private final static Searcher SEARCHER;

    static {
        try {
            ClassPathResource resource = new ClassPathResource("ip2region.xdb");
            byte[] cBuff;

            if ("jar".equals(resource.getURL().getProtocol())) {
                // 文件在 JAR 包中，使用 InputStream 加载
                try (InputStream inputStream = resource.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    cBuff = outputStream.toByteArray();
                }
            } else {
                // 文件在文件系统中，使用 Files 读取
                String path = resource.getFile().getPath();
                cBuff = Files.readAllBytes(Paths.get(path));

//                String path = resource.getURL().getPath();
//                byte[] cBuff = Searcher.loadContentFromFile(path);
            }

            // 使用缓冲区初始化 Searcher
            SEARCHER = Searcher.newWithBuffer(cBuff);
            log.info("加载了ip2region.xdb文件, Searcher初始化完成!");
        } catch (Exception e) {
            log.error("初始化ip2region.xdb文件失败,报错信息:[{}]", e.getMessage(), e);
            throw new RuntimeException("系统异常!");
        }
    }


    /**
     * 解析ip地址
     *
     * @param ipStr 字符串类型ip 例:192.168.0.1
     * @return 返回结果形式(国家 | 区域 | 省份 | 城市 | ISP) 例 [中国, 0, 河北省, 衡水市, 电信]
     */
    public static List<String> parse(String ipStr) {
        return parse(ipStr, null);
    }

    /**
     * 自定义解析ip地址
     *
     * @param ipStr ip 字符串类型ip 例:1970753539(经过转换后的)
     * @param index 想要获取的区间 例如:只想获取 省,市 index = [2,3]
     * @return 返回结果例 [北京,北京市]
     */
    public static List<String> parse(String ipStr, int[] index) {
        try {
            long ip = Searcher.checkIP(ipStr);
            return parse(ip, index);
        } catch (Exception e) {
            log.error("ip解析为long错误,ipStr:[{}],错误信息:[{}]", ipStr, e.getMessage(), e);
            throw new RuntimeException("系统异常!");
        }
    }

    /**
     * 自定义解析ip地址
     *
     * @param ip    ip Long类型ip
     * @param index 想要获取的区间 例如:只想获取 省,市 index = [2,3]
     * @return 返回结果例 [河北省, 衡水市]
     */
    public static List<String> parse(Long ip, int[] index) {
        //获取xdb文件资源
        List<String> regionList = new ArrayList<>();
        try {
            String region = SEARCHER.search(ip);
            String[] split = region.split("\\|");
            if (index == null) {
                regionList = Arrays.asList(split);
            } else {
                for (int i : index) {
                    regionList.add(split[i]);
                }
            }
            //关闭资源
            SEARCHER.close();
        } catch (Exception e) {
            log.error("根据ip解析地址失败,ip:[{}],index[{}],报错信息:[{}]", ip, index, e.getMessage(), e);
            throw new RuntimeException("系统异常!");
        }
        return regionList;
    }


}
