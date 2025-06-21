package com.na.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class NaFileReadUtil {
    public static String getFileAbsolutePath(String filePath) throws IOException {
        Resource resource = new ClassPathResource(filePath);
//        String osName = System.getProperty("os.name").toLowerCase();
//        String fullPath = osName.startsWith("win") ? resource.getFile().getAbsolutePath() : getJarFilePath(filePath);
//        return fullPath;
        return getJarFilePath(filePath);
    }

    /**
     * 从 JAR 包中的资源路径读取指定文件，并复制到项目当前工作目录中对应的位置。
     * 如果目标文件所在目录不存在，则自动创建。
     * 如果目标文件不存在或大小为 0，则从 JAR 包中的资源提取并复制到工作目录。
     *
     * @param filePath 类路径下资源文件的相对路径（如 "fonts/NotoSerifSC-Regular.ttf"）
     * @return 复制后的文件绝对路径；如果出错，返回空字符串
     */
    private static String getJarFilePath(String filePath) {
        try {
            // 获取当前工作目录（JAR 所在目录）
            Path currentWorkingDir = Paths.get(System.getProperty("user.dir"));

            // 构造目标文件路径
            Path targetPath = currentWorkingDir.resolve(filePath);
            File targetFile = targetPath.toFile();

            // 确保上级目录存在
            if (!targetFile.getParentFile().exists()) {
                boolean created = targetFile.getParentFile().mkdirs();
                if (created) {
                    System.out.println("目录已创建: " + targetFile.getParentFile());
                }
            }

            // 如果目标文件不存在或为空，则从 classpath 中提取资源复制
            if (!targetFile.exists() || targetFile.length() == 0) {
                System.out.println("准备复制资源文件到: " + targetFile.getAbsolutePath());

                // 注意：路径必须是相对 resources 根目录的路径
                try (InputStream in = NaFileReadUtil.class.getClassLoader().getResourceAsStream(filePath)) {
                    if (in == null) {
                        System.err.println("资源未找到: " + filePath);
                        return "";
                    }
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("资源复制完成: " + targetFile.getAbsolutePath());
                }
            }

            return targetPath.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
