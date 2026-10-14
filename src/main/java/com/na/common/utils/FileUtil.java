package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 文件操作工具类: Base64 与图片互转、目录/文件创建删除、classpath 资源提取、上传文件校验
 * <p>
 * 示例:
 * <pre>{@code
 * // 图片转 Base64(带 data URI 前缀, 可直接放入 <img src>)
 * String dataUri = FileUtil.convertImageToBase64("https://x.com/a.png", true);
 * // Base64 还原为字节(自动兼容 "data:image/png;base64," 前缀)
 * byte[] bytes = FileUtil.convertBase64ToImageBytes(dataUri);
 * // 按日期生成存储路径: 2026/09/03/
 * String path = FileUtil.generateDateStoragePath();
 * }</pre>
 */
@Slf4j
public final class FileUtil {

    /** Base64 data URI 前缀标记 */
    private static final String DATA_URI_PREFIX = "base64,";

    /** 常见图片扩展名(isImage 按文件名兜底判断时使用) */
    private static final List<String> IMAGE_SUFFIXES = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    /** 网络资源读取默认连接超时(毫秒) */
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    /** 网络资源读取默认读取超时(毫秒) */
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    /** 纯静态工具类, 禁止实例化 */
    private FileUtil() {
    }

    /**
     * 将图片(本地路径或网络地址)转换为 Base64 字符串(不含 data URI 前缀)
     * <p>
     * 示例: {@code FileUtil.convertImageToBase64("D:/img/a.png")}
     *
     * @param path 图片路径, 支持本地路径或 http(s) 地址; 空串返回空结果
     * @return Base64 字符串; 输入为空时返回空字符串
     * @throws IOException 本地文件读取失败或网络请求失败时抛出
     */
    public static String convertImageToBase64(String path) throws IOException {
        return convertImageToBase64(path, false);
    }

    /**
     * 将图片(本地路径或网络地址)转换为 Base64 字符串, 可选择携带 data URI 前缀
     * <p>
     * 示例:
     * <pre>{@code
     * // 结果形如 "data:image/png;base64,iVBORw0KG..." 可直接用于前端 <img src>
     * String dataUri = FileUtil.convertImageToBase64("https://x.com/a.png", true);
     * }</pre>
     *
     * @param path             图片路径, 支持本地路径或 http(s) 地址
     * @param withDataUriPrefix true 时输出带 "data:image/png;base64," 风格前缀(用于前端直接展示)
     * @return Base64 字符串; 输入为空时返回空字符串
     * @throws IOException 读取失败时抛出
     */
    public static String convertImageToBase64(String path, boolean withDataUriPrefix) throws IOException {
        if (!StringUtils.hasText(path)) {
            return "";
        }

        byte[] imageBytes = isLocalPath(path)
                ? Files.readAllBytes(Paths.get(path))
                : readBytesFromURL(path);

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return withDataUriPrefix ? buildDataUriPrefix(path) + base64 : base64;
    }

    /**
     * 将 Base64 字符串还原为图片字节数组
     * <p>
     * 自动兼容两种输入: 纯 Base64 串, 或浏览器常见的 "data:image/png;base64,xxx" 格式
     * <p>
     * 示例: {@code byte[] bytes = FileUtil.convertBase64ToImageBytes(dataUri);}
     *
     * @param base64Image Base64 字符串(允许带 data URI 前缀); 输入为空/空白时返回空数组
     * @return 解码后的字节数组; 输入非法 Base64 时抛出 IllegalArgumentException
     */
    public static byte[] convertBase64ToImageBytes(String base64Image) {
        if (!StringUtils.hasText(base64Image)) {
            return new byte[0];
        }
        // 兼容 "data:image/png;base64,xxxx" 格式: 截取逗号之后的纯 Base64 部分
        int commaIndex = base64Image.indexOf(DATA_URI_PREFIX);
        String pureBase64 = commaIndex >= 0 ? base64Image.substring(commaIndex + DATA_URI_PREFIX.length()) : base64Image;
        return Base64.getDecoder().decode(pureBase64);
    }

    /**
     * 判断路径是否为本地路径(非 http/https 开头)
     *
     * @param path 输入路径
     * @return true 表示本地路径; false 表示网络 URL; null 视为本地路径
     */
    public static boolean isLocalPath(String path) {
        return (path == null || !CommonUtil.ishttp(path));
    }

    /**
     * 读取网络资源字节内容(内置连接 5s / 读取 30s 超时, 防止对端无响应时线程无限挂起)
     * <p>
     * 安全约定: 仅允许 http/https 协议, 传入 file:/ftp: 等其他协议会抛出 IllegalArgumentException,
     * 防止该方法被间接用于读取本地文件
     * <p>
     * 示例: {@code byte[] bytes = FileUtil.readBytesFromURL("https://x.com/a.png");}
     *
     * @param urlPath 网络地址(仅 http/https)
     * @return 资源字节数组
     * @throws IOException          连接失败、超时或读取失败时抛出
     * @throws IllegalArgumentException 地址为空、格式非法或协议非 http/https 时抛出
     */
    public static byte[] readBytesFromURL(String urlPath) throws IOException {
        // 协议白名单校验, 禁止 file:/ftp: 等协议绕过工具类读取本地/其他资源
        String scheme = URI.create(urlPath).getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("readBytesFromURL 仅支持 http/https 地址: " + urlPath);
        }
        URLConnection connection = URI.create(urlPath).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        try (InputStream inputStream = connection.getInputStream()) {
            // StreamUtils 替代手写 1024 缓冲区循环(JDK 8 语言级别兼容)
            return StreamUtils.copyToByteArray(inputStream);
        }
    }

    /**
     * 基于当前工作目录创建相对路径目录(已存在时不做任何操作)
     * <p>
     * 示例: {@code Path dir = FileUtil.createWorkDirectory("upload/images");}
     *
     * @param relativePath 相对路径(如 "upload/images")
     * @return 创建(或已存在)的目录绝对 Path
     * @throws RuntimeException 目录创建失败时抛出
     */
    public static Path createWorkDirectory(String relativePath) {
        Path targetPath = Paths.get(System.getProperty("user.dir"), relativePath);
        return createDirectory(targetPath);
    }

    /**
     * 创建绝对路径目录(已存在时不做任何操作)
     * <p>
     * 示例: {@code Path dir = FileUtil.createAbsDirectory("D:/data/tmp");}
     *
     * @param absolutePath 绝对路径(如 "D:/data/tmp")
     * @return 创建(或已存在)的目录 Path
     * @throws RuntimeException 目录创建失败时抛出
     */
    public static Path createAbsDirectory(String absolutePath) {
        return createDirectory(Paths.get(absolutePath));
    }

    /**
     * 通用目录创建: 不存在时递归创建多级目录, 已存在时直接返回
     *
     * @param path 目录路径
     * @return 创建(或已存在)的目录 Path
     * @throws RuntimeException 目录创建失败时抛出
     */
    public static Path createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("目录已创建: {}", path);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建目录失败: " + path, e);
        }
        return path;
    }

    /**
     * 根据文件路径自动创建父目录和文件(若不存在; 已存在时直接返回成功)
     * <p>
     * 示例: {@code boolean ok = FileUtil.createDirectoryAndFile("D:/temp/data/test.txt");}
     *
     * @param filePathStr 文件全路径(如 "D:/temp/data/test.txt"); 允许仅文件名(基于工作目录解析)
     * @return true 表示创建成功或已存在; false 表示创建失败
     */
    public static boolean createDirectoryAndFile(String filePathStr) {
        if (!StringUtils.hasText(filePathStr)) {
            return false;
        }
        Path filePath = Paths.get(filePathStr);
        try {
            // 修复原实现 NPE: 纯文件名(如 "test.txt")的 getParent() 为 null
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("目录创建成功: {}", parent);
            }

            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                log.info("文件创建成功: {}", filePath);
            } else {
                log.info("文件已存在: {}", filePath);
            }
            return true;
        } catch (IOException e) {
            log.error("创建文件失败: {}", filePathStr, e);
            return false;
        }
    }

    /**
     * 确保某个文件路径的父目录存在(不存在则创建); 传入目录路径时确保该目录本身存在
     * <p>
     * 示例: {@code boolean ok = FileUtil.checkDirectory("D:/temp/data/test.txt");}
     *
     * @param filePath 文件路径(含文件名)或目录路径
     * @return true 表示目录已存在或创建成功; 路径为空/非法或创建失败返回 false
     */
    public static boolean checkDirectory(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return false;
        }
        try {
            Path path = Paths.get(filePath);
            // 传入目录时确保其自身存在; 传入文件路径时取父目录
            Path dir = Files.isDirectory(path) ? path : path.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return true;
        } catch (IOException | InvalidPathException e) {
            // 路径非法(InvalidPathException)或创建失败均视为目录不可用
            log.error("确保目录存在失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 删除指定路径的文件
     * <p>
     * 注意: 目录删除要求目录为空, 非空目录会返回删除失败
     * <p>
     * 示例: {@code String result = FileUtil.deleteFile("D:/temp/a.txt");}
     *
     * @param filePath 要删除的文件路径
     * @return 操作结果描述(中文文案, 可直接用于提示)
     */
    public static String deleteFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return "路径为空";
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return "文件不存在: " + filePath;
        }

        boolean deleted = file.delete();
        if (deleted) {
            log.info("文件删除成功: {}", filePath);
        } else {
            log.warn("文件删除失败(目录非空或被占用): {}", filePath);
        }
        return deleted ? "文件删除成功: " + filePath : "文件删除失败: " + filePath;
    }

    /**
     * 判断上传文件是否为图片
     * <p>
     * 判断规则: 优先基于 Content-Type 前缀 image/ 判断;
     * Content-Type 缺失或为通用二进制流(application/octet-stream)时, 按文件扩展名兜底判断
     * <p>
     * 示例: {@code if (FileUtil.isImage(multipartFile)) { ... }}
     *
     * @param file MultipartFile 上传文件
     * @return true 表示是图片类型; file 为 null 或扩展名也无法识别时返回 false
     */
    public static boolean isImage(MultipartFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return true;
        }
        // 兜底: 部分浏览器/客户端上传图片时 Content-Type 为空或 octet-stream, 按扩展名判断
        String filename = file.getOriginalFilename();
        String lower = filename == null ? "" : filename.toLowerCase();
        return IMAGE_SUFFIXES.stream().anyMatch(lower::endsWith);
    }

    /**
     * 获取 classpath 资源文件的可用磁盘路径
     * <p>
     * 打包为 JAR 后资源在包内无法直接以 File 方式访问, 本方法将资源提取到
     * 当前工作目录(仅首次或目标文件为空时提取), 返回提取后的磁盘绝对路径
     * <p>
     * 示例: {@code String fontPath = FileUtil.getFileAbsolutePath("fonts/NotoSerifSC-Regular.ttf");}
     *
     * @param filePath 类路径下资源相对路径(相对 resources 根目录, 如 "fonts/NotoSerifSC-Regular.ttf")
     * @return 可直接用于磁盘 IO 的文件绝对路径; 资源不存在或提取失败返回空字符串
     * @throws IOException 兼容历史签名保留(当前实现不会主动抛出)
     */
    public static String getFileAbsolutePath(String filePath) throws IOException {
        return getJarFilePath(filePath);
    }

    /**
     * 将 classpath 资源提取到当前工作目录并返回其磁盘路径
     * <p>
     * 提取规则: 目标文件不存在或大小为 0 时, 从 classpath 复制覆盖;
     * 已存在且非空时直接复用, 避免每次启动重复写盘
     *
     * @param filePath 类路径下资源相对路径
     * @return 提取后的文件绝对路径; 资源不存在或失败返回空字符串
     */
    private static String getJarFilePath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return "";
        }
        try {
            // 当前工作目录(JAR 所在目录), 资源将提取到此处
            Path currentWorkingDir = Paths.get(System.getProperty("user.dir"));
            Path targetPath = currentWorkingDir.resolve(filePath);
            File targetFile = targetPath.toFile();

            // 确保上级目录存在(纯文件名时 getParentFile 为工作目录本身, 已存在无需创建)
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                log.error("创建资源目录失败: {}", parentDir.getAbsolutePath());
                return "";
            }

            // 目标文件不存在或为空时, 从 classpath 提取复制
            if (!targetFile.exists() || targetFile.length() == 0) {
                log.info("准备复制资源文件到: {}", targetFile.getAbsolutePath());
                try (InputStream in = FileUtil.class.getClassLoader().getResourceAsStream(filePath)) {
                    if (in == null) {
                        log.error("资源未找到: {}", filePath);
                        return "";
                    }
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("资源复制完成: {}", targetFile.getAbsolutePath());
                }
            }

            return targetPath.toString();
        } catch (Exception e) {
            log.error("提取 classpath 资源失败: {}", filePath, e);
            return "";
        }
    }

    /**
     * 根据路径推断图片类型并拼接 data URI 前缀(仅识别常见类型, 未识别时默认 png)
     */
    private static String buildDataUriPrefix(String path) {
        String lower = path.toLowerCase();
        String mime;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mime = "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            mime = "image/gif";
        } else if (lower.endsWith(".webp")) {
            mime = "image/webp";
        } else if (lower.endsWith(".bmp")) {
            mime = "image/bmp";
        } else {
            mime = "image/png";
        }
        return "data:" + mime + ";base64,";
    }
}
