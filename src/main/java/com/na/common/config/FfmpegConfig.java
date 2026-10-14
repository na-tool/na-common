package com.na.common.config;

import com.na.common.utils.VideoConvertUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FFmpeg 配置类: 读取 application.yml 中的 {@code na.ffmpeg.*} 配置并注入 {@link VideoConvertUtil} 静态工具类
 * <p>
 * <b>生效条件(默认开启):</b> 显式配 {@code na.ffmpeg.enable=true} 或不配都生效; 设 {@code na.ffmpeg.enable=false} 可关闭
 * <p>
 * yml 配置示例:
 * <pre>{@code
 * na:
 *   ffmpeg:
 *     enable: true              # 是否启用此配置注入(默认 true)
 *     path: /opt/ffmpeg/bin/ffmpeg   # FFmpeg 可执行文件完整路径(覆盖 OS 默认值)
 *     timeout-minutes: 5         # 进程执行超时(分钟), 默认 2
 *     hls-time: 15              # HLS 切片时长(秒), 默认 10
 * }</pre>
 * <p>
 * 注入时机: Spring 容器启动完成所有属性绑定后, 通过 {@link PostConstruct} 调用
 * {@link VideoConvertUtil#applyConfig(String, Integer, Integer)} 写入工具类的 volatile 静态字段
 */
@Slf4j
@Data
@ConditionalOnProperty(name = "na.ffmpeg.enable", havingValue = "true", matchIfMissing = true)
@ConfigurationProperties(prefix = "na.ffmpeg")
public class FfmpegConfig {

    /**
     * FFmpeg 可执行文件完整路径
     * <p>
     * 不配置时按操作系统选择默认值:
     * <ul>
     *   <li>Windows: {@code D:\ffmpeg-7.1.1-full_build\bin\ffmpeg.exe}</li>
     *   <li>Linux/Mac: {@code ffmpeg}(依赖 PATH 环境变量)</li>
     * </ul>
     */
    private String path;

    /**
     * 进程执行超时(分钟); <=0 时回落默认 2 分钟
     */
    private Integer timeoutMinutes;

    /**
     * HLS 切片时长(秒); <=0 时回落默认 10 秒
     */
    private Integer hlsTime;

    /**
     * 启动后将 yml 配置注入 {@link VideoConvertUtil} 静态工具类
     * <p>
     * <b>关联链路:</b> FfmpegConfig 创建 → @ConfigurationProperties 绑定 na.ffmpeg.* 属性 → @PostConstruct init()
     * → 调用 {@link VideoConvertUtil#applyConfig(String, Integer, Integer)} 写入工具类 volatile 静态字段
     * → 后续所有 {@link VideoConvertUtil#convertMp4ToM3u8} 调用优先读取该配置
     * <p>
     * 任一字段为 null 表示不覆盖, 由工具类走后续的 系统属性/环境变量/默认值 解析
     */
    @PostConstruct
    public void init() {
        log.info("[FfmpegConfig] na.ffmpeg.* 配置注入 VideoConvertUtil 静态字段: path={}, timeoutMinutes={}, hlsTime={}",
                path, timeoutMinutes, hlsTime);
        VideoConvertUtil.applyConfig(path, timeoutMinutes, hlsTime);
    }
}
