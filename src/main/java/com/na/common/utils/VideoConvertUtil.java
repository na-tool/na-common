package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 视频转换工具类: 基于 FFmpeg 实现 MP4 转 M3U8(HLS 切片), 以及 M3U8/TS 切片文件的统计与解析
 * <p>
 * <b>关联 Spring 配置:</b> Spring Boot 启动时由 {@link com.na.common.config.FfmpegConfig}
 * 通过 {@code @PostConstruct init()} 自动调用 {@link #applyConfig(String, Integer, Integer)}
 * 把 yml 的 {@code na.ffmpeg.*} 配置注入到本工具类的 volatile 静态字段, 无需业务代码显式处理.
 * <p>
 * <b>系统适配:</b> 自动识别 Windows / Linux / Mac 并选择对应的 FFmpeg 可执行文件, 无需调用方关心操作系统差异
 * <p>
 * <b>配置项(优先级从高到低):</b>
 * <table border="1">
 *   <tr><th>配置方式</th><th>FFmpeg 路径</th><th>超时(分钟)</th><th>切片时长(秒)</th></tr>
 *   <tr><td>Spring yml</td><td>{@code na.ffmpeg.path}</td><td>{@code na.ffmpeg.timeout-minutes}</td><td>{@code na.ffmpeg.hls-time}</td></tr>
 *   <tr><td>JVM 系统属性</td><td>-Dna.ffmpeg.path=...</td><td>-Dna.ffmpeg.timeout-minutes=5</td><td>-Dna.ffmpeg.hls-time=15</td></tr>
 *   <tr><td>环境变量</td><td>FFMPEG_PATH=...</td><td>FFMPEG_TIMEOUT_MINUTES=5</td><td>FFMPEG_HLS_TIME=15</td></tr>
 *   <tr><td>默认值</td><td>Windows: D:\ffmpeg-7.1.1-full_build\bin\ffmpeg.exe<br>Linux/Mac: ffmpeg(PATH 中)</td><td>2</td><td>10</td></tr>
 * </table>
 * <p>
 * 示例:
 * <pre>{@code
 * // MP4 转 M3U8(command 为 null 时使用默认命令: -codec copy 不转码直接切片, hls-time 按 yml/配置取)
 * boolean ok = VideoConvertUtil.convertMp4ToM3u8("D:/video/01.mp4", "D:/video/hls/output.m3u8", null);
 * // 自定义完整命令(覆盖默认命令, command 非 null 时 mp4FilePath/m3u8FilePath 可为 null)
 * VideoConvertUtil.convertMp4ToM3u8(null, null,
 *         "ffmpeg -i \"D:/in.mp4\" -hls_time 5 -f hls \"D:/out/index.m3u8\"");
 * // 统计切片数量
 * int count = VideoConvertUtil.countTsFiles("D:/video/hls/output.m3u8");
 * // 提取全部 TS 切片完整路径
 * List<String> tsFiles = VideoConvertUtil.getTsFilesFromM3U8("D:/video/hls/output.m3u8");
 * }</pre>
 *
 * @see com.na.common.config.FfmpegConfig
 */
@Slf4j
public final class VideoConvertUtil {

    /** FFmpeg 可执行文件路径的 JVM 系统属性名: -Dna.ffmpeg.path=/usr/local/bin/ffmpeg */
    private static final String FFMPEG_PATH_PROPERTY = "na.ffmpeg.path";

    /** FFmpeg 可执行文件路径的环境变量名: FFMPEG_PATH=/usr/local/bin/ffmpeg */
    private static final String FFMPEG_PATH_ENV = "FFMPEG_PATH";

    /** 进程超时(分钟)的系统属性名 */
    private static final String TIMEOUT_PROPERTY = "na.ffmpeg.timeout-minutes";

    /** 进程超时(分钟)的环境变量名 */
    private static final String TIMEOUT_ENV = "FFMPEG_TIMEOUT_MINUTES";

    /** HLS 切片时长(秒)的系统属性名 */
    private static final String HLS_TIME_PROPERTY = "na.ffmpeg.hls-time";

    /** HLS 切片时长(秒)的环境变量名 */
    private static final String HLS_TIME_ENV = "FFMPEG_HLS_TIME";

    /** Windows 下 FFmpeg 默认安装路径 */
    private static final String WINDOWS_FFMPEG_PATH = "D:\\ffmpeg-7.1.1-full_build\\bin\\ffmpeg.exe";

    /** FFmpeg 进程默认超时时间(分钟) */
    private static final int DEFAULT_TIMEOUT_MINUTES = 2;

    /** HLS 默认切片时长(秒) */
    private static final int HLS_SEGMENT_TIME_SECONDS = 10;

    // ---- 运行时可变配置(由 FfmpegConfig 在 Spring 启动时或手动 applyConfig 注入) ----

    /** yml/手动配置的 FFmpeg 路径; null 表示未配置, 回落系统属性/环境变量/默认值 */
    private static volatile String configuredFfmpegPath;

    /** yml/手动配置的超时(分钟); null 表示未配置 */
    private static volatile Integer configuredTimeoutMinutes;

    /** yml/手动配置的切片时长(秒); null 表示未配置 */
    private static volatile Integer configuredHlsTimeSeconds;

    /** 纯静态工具类, 禁止实例化 */
    private VideoConvertUtil() {
    }

    /**
     * 手动注入配置(也由 FfmpegConfig 在 Spring 启动时基于 yml 配置自动调用)
     * <p>
     * 任一参数传 null 表示不覆盖, 沿用原解析值; 调用方也可在不使用 Spring 的情况下手动注入
     * <p>
     * 示例:
     * <pre>{@code
     * VideoConvertUtil.applyConfig("/opt/ffmpeg/bin/ffmpeg", 5, 15);
     * }</pre>
     *
     * @param ffmpegPath        FFmpeg 可执行文件路径; null=不覆盖
     * @param timeoutMinutes    进程超时(分钟); null=不覆盖, <=0 不生效(回落默认)
     * @param hlsTimeSeconds    HLS 切片时长(秒); null=不覆盖, <=0 不生效(回落默认)
     */
    public static void applyConfig(String ffmpegPath, Integer timeoutMinutes, Integer hlsTimeSeconds) {
        if (StringUtils.hasText(ffmpegPath)) {
            configuredFfmpegPath = ffmpegPath.trim();
            log.info("FFmpeg路径已注入: {}", configuredFfmpegPath);
        }
        if (timeoutMinutes != null && timeoutMinutes > 0) {
            configuredTimeoutMinutes = timeoutMinutes;
            log.info("FFmpeg超时已注入: {} 分钟", configuredTimeoutMinutes);
        }
        if (hlsTimeSeconds != null && hlsTimeSeconds > 0) {
            configuredHlsTimeSeconds = hlsTimeSeconds;
            log.info("FFmpeg切片时长已注入: {} 秒", configuredHlsTimeSeconds);
        }
    }

    /**
     * MP4 转 M3U8(HLS 切片)
     * <p>
     * 默认命令: {@code ffmpeg -i <mp4> -codec: copy -hls_time <N> -f hls <output.m3u8>},
     * 不转码直接切片(速度快, 仅重新封装); 默认切片时长按 配置优先级 取 yml/系统属性/环境变量/默认值
     * <p>
     * <b>command 参数说明:</b>
     * <ul>
     *   <li>{@code command == null} 或空白 → <b>使用默认命令</b>(自动按配置选择 FFmpeg 路径、超时、切片时长)</li>
     *   <li>{@code command} 非空 → <b>完全使用你传入的命令</b>(此时 mp4FilePath 和 m3u8FilePath 可为 null, 不会校验)</li>
     * </ul>
     * <p>
     * 示例:
     * <pre>{@code
     * // 1. 走默认命令(最常用, 参数都来自 yml/系统属性)
     * boolean ok = VideoConvertUtil.convertMp4ToM3u8("D:/in.mp4", "D:/out/index.m3u8", null);
     *
     * // 2. 传完整命令覆盖(比如要加滤镜/水印/转码时)
     * VideoConvertUtil.convertMp4ToM3u8(null, null,
     *         "ffmpeg -i \"D:/in.mp4\" -vf scale=1280x720 -c:v libx264 -hls_time 5 -f hls \"D:/out/index.m3u8\"");
     * }</pre>
     *
     * @param mp4FilePath  输入 MP4 文件路径(如 D:/test.mp4); 传自定义 command 时可为 null
     * @param m3u8FilePath 输出 M3U8 文件路径(如 D:/output/test.m3u8); 传自定义 command 时可为 null
     * @param command      自定义 FFmpeg 命令; 为 null/空白时使用默认命令(自动适配 Windows/Linux)
     * @return true=转换成功; 输入文件不存在/目录创建失败/执行超时/进程异常时返回 false
     */
    public static boolean convertMp4ToM3u8(String mp4FilePath,
                                           String m3u8FilePath,
                                           String command) {
        // command 非 null/非空白 → 走自定义命令(跳过输入文件与输出目录校验, 由调用方保证正确性)
        if (StringUtils.hasText(command)) {
            return executeFfmpeg(splitCommand(command));
        }

        // 校验输入文件是否存在
        File inputFile = new File(mp4FilePath);
        if (!inputFile.exists()) {
            log.error("输入文件不存在: {}", mp4FilePath);
            return false;
        }

        // 确保输出目录存在(Files.createDirectories: 目录已存在时不报错, 失败时有明确异常)
        File outputFile = new File(m3u8FilePath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir.toPath());
            } catch (IOException e) {
                log.error("创建输出目录失败: {}", parentDir.getAbsolutePath(), e);
                return false;
            }
        }

        // 组装参数列表: -codec copy 不转码直接切片(速度快); -hls_list_size 0 保留全部切片索引
        // 用 List 传参(而非拼接字符串再 split), 路径含空格也不会被错误切分
        List<String> args = new ArrayList<>();
        args.add(resolveFfmpegPath());
        args.add("-i");
        args.add(mp4FilePath);
        args.add("-codec:");
        args.add("copy");
        args.add("-start_number");
        args.add("0");
        args.add("-hls_time");
        args.add(String.valueOf(resolveHlsTimeSeconds()));
        args.add("-hls_list_size");
        args.add("0");
        args.add("-f");
        args.add("hls");
        args.add(m3u8FilePath);
        return executeFfmpeg(args);
    }

    /**
     * 统计 M3U8 文件中记录的 TS 切片数量(按行解析, 非扫描磁盘)
     *
     * @param m3u8FilePath M3U8 文件完整路径(如 D:\mp4\hls\output.m3u8)
     * @return TS 切片数量; 文件不存在或读取失败时返回 0
     */
    public static int countTsFiles(String m3u8FilePath) {
        File m3u8File = new File(m3u8FilePath);
        if (!m3u8File.exists()) {
            return 0;
        }

        try (BufferedReader reader = Files.newBufferedReader(m3u8File.toPath(), StandardCharsets.UTF_8)) {
            int count = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                // 过滤空行和注释行(以#开头), 只统计 .ts 结尾的切片行
                String trimmed = line.trim();
                if (trimmed.endsWith(".ts") && !trimmed.startsWith("#")) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            log.error("读取M3U8文件失败: {}", m3u8FilePath, e);
            return 0;
        }
    }

    /**
     * 扫描目录统计磁盘上的 TS 文件数量
     *
     * @param outputDir 输出目录(M3U8 和 TS 所在目录, 如 D:\mp4\hls)
     * @return TS 切片数量; 目录不存在或不是目录时返回 0
     */
    public static int countTsInDir(String outputDir) {
        File dir = new File(outputDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        File[] tsFiles = dir.listFiles(file -> file.getName().endsWith(".ts"));
        return tsFiles == null ? 0 : tsFiles.length;
    }

    /**
     * 从 M3U8 文件中提取所有 TS 切片的完整路径
     * <p>
     * M3U8 内相对路径会基于 M3U8 所在目录解析为绝对路径
     *
     * @param m3u8Path M3U8 文件的绝对路径
     * @return TS 文件完整路径列表; 文件不存在时返回空列表
     */
    public static List<String> getTsFilesFromM3U8(String m3u8Path) {
        List<String> tsFiles = new ArrayList<>();
        File m3u8File = new File(m3u8Path);
        if (!m3u8File.exists()) {
            log.error("M3U8文件不存在: {}", m3u8Path);
            return tsFiles;
        }

        // M3U8 所在目录, 用于解析内部相对路径
        String m3u8Dir = m3u8File.getParent();

        try (BufferedReader reader = Files.newBufferedReader(m3u8File.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // 跳过注释行(#EXTM3U/#EXTINF 等标签)和空行
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.endsWith(".ts")) {
                    // 绝对路径(Linux: /开头; Windows: 盘符)直接使用, 相对路径基于 M3U8 目录解析
                    if (trimmed.startsWith("/") || trimmed.contains(":\\") || trimmed.contains(":/")) {
                        tsFiles.add(trimmed);
                    } else {
                        tsFiles.add(Paths.get(m3u8Dir, trimmed).toAbsolutePath().toString());
                    }
                }
            }
        } catch (IOException e) {
            log.error("读取M3U8文件失败: {}", m3u8Path, e);
        }
        return tsFiles;
    }

    /**
     * 解析 FFmpeg 可执行文件路径(跨平台自动适配)
     * <p>
     * 优先级: yml 配置 &gt; JVM 系统属性 na.ffmpeg.path &gt; 环境变量 FFMPEG_PATH &gt; 操作系统默认值
     * <p>
     * 操作系统默认值: Windows 取 {@link #WINDOWS_FFMPEG_PATH}, Linux/Mac 取 PATH 中的 ffmpeg
     *
     * @return FFmpeg 可执行文件路径
     */
    private static String resolveFfmpegPath() {
        // 1. yml/手动注入的配置(优先级最高)
        if (StringUtils.hasText(configuredFfmpegPath)) {
            return configuredFfmpegPath;
        }
        // 2. JVM 系统属性: -Dna.ffmpeg.path=...
        String customPath = System.getProperty(FFMPEG_PATH_PROPERTY);
        if (StringUtils.hasText(customPath)) {
            return customPath;
        }
        // 3. 环境变量: FFMPEG_PATH=...
        customPath = System.getenv(FFMPEG_PATH_ENV);
        if (StringUtils.hasText(customPath)) {
            return customPath;
        }
        // 4. 操作系统默认: Windows 默认安装路径; Linux/Mac 假设已加入 PATH
        return CommonUtil.isWindows() ? WINDOWS_FFMPEG_PATH : "ffmpeg";
    }

    /**
     * 解析 FFmpeg 进程超时时间(分钟)
     * <p>
     * 优先级: yml 配置 &gt; JVM 系统属性 &gt; 环境变量 &gt; 默认 2 分钟; 配置非法或 &lt;=0 时回落默认值
     *
     * @return 超时分钟数
     */
    private static int resolveTimeoutMinutes() {
        // 1. yml/手动注入
        if (configuredTimeoutMinutes != null && configuredTimeoutMinutes > 0) {
            return configuredTimeoutMinutes;
        }
        // 2. JVM 系统属性 + 环境变量
        String value = System.getProperty(TIMEOUT_PROPERTY, System.getenv(TIMEOUT_ENV));
        if (StringUtils.hasText(value)) {
            try {
                int minutes = Integer.parseInt(value.trim());
                if (minutes > 0) {
                    return minutes;
                }
            } catch (NumberFormatException e) {
                log.warn("超时配置非法: {}, 使用默认 {} 分钟", value, DEFAULT_TIMEOUT_MINUTES);
            }
        }
        return DEFAULT_TIMEOUT_MINUTES;
    }

    /**
     * 解析 HLS 切片时长(秒)
     * <p>
     * 优先级: yml 配置 &gt; JVM 系统属性 &gt; 环境变量 &gt; 默认 10 秒; 配置非法或 &lt;=0 时回落默认值
     *
     * @return 切片时长秒数
     */
    private static int resolveHlsTimeSeconds() {
        // 1. yml/手动注入
        if (configuredHlsTimeSeconds != null && configuredHlsTimeSeconds > 0) {
            return configuredHlsTimeSeconds;
        }
        // 2. JVM 系统属性 + 环境变量
        String value = System.getProperty(HLS_TIME_PROPERTY, System.getenv(HLS_TIME_ENV));
        if (StringUtils.hasText(value)) {
            try {
                int seconds = Integer.parseInt(value.trim());
                if (seconds > 0) {
                    return seconds;
                }
            } catch (NumberFormatException e) {
                log.warn("切片时长配置非法: {}, 使用默认 {} 秒", value, HLS_SEGMENT_TIME_SECONDS);
            }
        }
        return HLS_SEGMENT_TIME_SECONDS;
    }


    /**
     * 将命令字符串切分为参数列表(支持双引号包裹含空格的路径)
     * <p>
     * 简单的 {@code split(" ")} 会把 {@code "D:/my video/in.mp4"} 错误切分为两段;
     * 本方法把双引号内的内容视为一个参数并去掉引号
     * <p>
     * 示例: {@code ffmpeg -i "D:/my video/in.mp4"} &rarr; [ffmpeg, -i, D:/my video/in.mp4]
     *
     * @param command 完整命令字符串
     * @return 参数列表(可直接传给 ProcessBuilder)
     */
    private static List<String> splitCommand(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : command.toCharArray()) {
            if (c == '"') {
                // 引号切换状态, 引号本身不保留
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                // 引号外的空格为参数分隔符
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        // 收尾: 追加最后一个参数
        if (current.length() > 0) {
            args.add(current.toString());
        }
        return args;
    }

    /**
     * 执行 FFmpeg 命令并等待完成
     * <p>
     * 输出流由 StreamGobbler 异步消费, 防止 FFmpeg 因缓冲区写满而阻塞
     *
     * @param commandArgs FFmpeg 命令参数列表(第一个元素为可执行文件路径)
     * @return true=进程在超时时间内正常退出(退出码 0)
     */
    private static boolean executeFfmpeg(List<String> commandArgs) {
        log.info("执行FFmpeg命令: {}", String.join(" ", commandArgs));
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(commandArgs);
            // 合并 stderr 到 stdout, 统一由一个 Gobbler 消费
            builder.redirectErrorStream(true);
            process = builder.start();

            // 异步消费输出流, 避免 FFmpeg 输出填满缓冲区导致进程挂起
            Thread gobbler = new StreamGobbler(process.getInputStream());
            gobbler.setDaemon(true);
            gobbler.start();

            // 限时等待: 超时强杀进程, 防止僵尸进程占用资源
            int timeoutMinutes = resolveTimeoutMinutes();
            boolean completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                log.error("FFmpeg执行超时({}分钟), 已强制终止", timeoutMinutes);
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg执行失败, 退出码: {}", exitCode);
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            // 恢复中断标记, 交给上层中断处理逻辑
            Thread.currentThread().interrupt();
            log.error("FFmpeg执行被中断", e);
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        } catch (IOException e) {
            log.error("FFmpeg命令执行异常(请检查ffmpeg路径是否正确): {}", String.join(" ", commandArgs), e);
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * FFmpeg 输出流异步消费线程
     * <p>
     * 逐行读取进程输出并写入日志, 防止输出缓冲区满导致 FFmpeg 阻塞
     */
    @Slf4j
    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[FFMPEG] {}", line);
                }
            } catch (IOException e) {
                log.error("读取FFmpeg输出流失败", e);
            }
        }
    }
}
