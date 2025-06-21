package com.na.common.utils;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


public class NaFileUtil {

    /**
     * 将图片（本地或网络）转换为 Base64 字符串
     *
     * @param path 图片路径，可以是本地路径或 http(s) 地址
     * @return base64 字符串
     * @throws IOException 读取失败抛出异常
     */
    public static String convertImageToBase64(String path) throws IOException {
        if (StringUtils.isEmpty(path)) {
            return "";
        }

        byte[] imageBytes = isLocalPath(path)
                ? Files.readAllBytes(Paths.get(path))
                : readBytesFromURL(path);

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 将 Base64 字符串还原为图片字节数组
     *
     * @param base64Image base64 字符串
     * @return 字节数组
     */
    public static byte[] convertBase64ToImageBytes(String base64Image) {
        return Base64.getDecoder().decode(base64Image);
    }

    /**
     * 判断路径是否为本地路径（非 http/https）
     *
     * @param path 输入路径
     * @return true 表示本地路径；false 表示 URL
     */
    public static boolean isLocalPath(String path) {
        return path != null && !path.startsWith("http://") && !path.startsWith("https://");
    }

    /**
     * 读取网络图片字节内容
     *
     * @param urlPath 网络地址
     * @return 字节数组
     * @throws IOException 读取失败抛出异常
     */
    public static byte[] readBytesFromURL(String urlPath) throws IOException {
        URL url = new URL(urlPath);
        try (InputStream inputStream = url.openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            return outputStream.toByteArray();
        }
    }

    /**
     * 创建相对路径目录（基于当前工作目录）
     *
     * @param relativePath 相对路径（如 "upload/images"）
     * @return 创建后的目录 Path
     */
    public static Path createWorkDirectory(String relativePath) {
        Path targetPath = Paths.get(System.getProperty("user.dir"), relativePath);
        return createDirectory(targetPath);
    }

    /**
     * 创建绝对路径目录
     *
     * @param absolutePath 绝对路径（如 D:/data/tmp）
     * @return 创建后的目录 Path
     */
    public static Path createAbsDirectory(String absolutePath) {
        return createDirectory(Paths.get(absolutePath));
    }

    /**
     * 根据文件路径自动创建目录和文件（若不存在）
     *
     * @param filePathStr 文件全路径（如 D:/temp/data/test.txt）
     * @return true 表示创建成功或已存在；false 表示失败
     */
    public static boolean createDirectoryAndFile(String filePathStr) {
        Path filePath = Paths.get(filePathStr);
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
                System.out.println("目录创建成功！");
            }

            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                System.out.println("文件创建成功！");
            } else {
                System.out.println("文件已存在！");
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 确保某个文件路径的父目录存在（若不存在则创建）
     *
     * @param filePath 文件路径（含文件名）
     * @return true 表示存在或创建成功；false 表示失败
     */
    public static boolean checkDirectory(String filePath) {
        if (StringUtils.isEmpty(filePath)) return false;

        File file = new File(filePath);
        File parentDir = file.isDirectory() ? file : file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                System.err.println("创建目录失败：" + parentDir.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    /**
     * 删除指定路径的文件
     *
     * @param filePath 要删除的文件路径
     * @return 操作结果描述
     */
    public static String deleteFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return "路径为空";
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return "文件不存在: " + filePath;
        }

        return file.delete() ? "文件删除成功: " + filePath : "文件删除失败: " + filePath;
    }

    /**
     * 生成 yyyy/MM/dd/ 格式的日期路径
     *
     * @return 日期路径字符串
     */
    public static String generateDateStoragePath() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/");
        return now.format(formatter);
    }

    /**
     * 判断上传文件是否是图片
     *
     * @param file MultipartFile 上传文件
     * @return true 表示是图片类型
     */
    public static boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 私有通用目录创建方法
     *
     * @param path 目录路径
     * @return Path 对象
     */
    private static Path createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("目录已创建: " + path);
            } else {
                System.out.println("目录已存在: " + path);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建目录失败: " + path, e);
        }
        return path;
    }
}
