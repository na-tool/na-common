package com.na.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NaVideoConvertUtil {

    /**
     * MP4转M3U8
     *
     * @param mp4FilePath  输入MP4文件名（含路径，如 D://test.mp4）
     * @param m3u8FilePath 输出M3U8文件名（含路径，如 D://output/test.m3u8）
     * @param command      FFmpeg转换命令（可选，如果为空则使用默认参数）
     * @return 转换是否成功
     */
    public static boolean convertMp4ToM3u8(String mp4FilePath,
                                           String m3u8FilePath,
                                           String command) {

        // 校验输入文件是否存在
        File inputFile = new File(mp4FilePath);
        if (!inputFile.exists()) {
            System.err.println("输入文件不存在：" + mp4FilePath);
            return false;
        }

        // 确保输出目录存在
        File outputFile = new File(m3u8FilePath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("创建输出目录失败：" + parentDir.getAbsolutePath());
                return false;
            }
        }

        // 使用默认命令
        if (StringUtils.isEmpty(command)) {
            // 判断操作系统，自动选择 ffmpeg 命令
            String ffmpegPath = "ffmpeg"; // 默认假设已添加到系统 PATH

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                ffmpegPath = "D:\\ffmpeg-7.1.1-full_build\\bin\\ffmpeg.exe";
            }
            command = String.format(
                    "%s -i \"%s\" -codec: copy -start_number 0 -hls_time 10 -hls_list_size 0 -f hls \"%s\"",
                    ffmpegPath, mp4FilePath, m3u8FilePath
            );
        }

        try {
            System.out.println("执行命令：" + command);
//            Process process = Runtime.getRuntime().exec(command);
            ProcessBuilder builder = new ProcessBuilder(command.split(" "));
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // 读取并输出 FFmpeg 的输出流（建议加上否则可能阻塞）
            StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "FFMPEG-LOG");
            StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "FFMPEG-LOG");
            errorGobbler.start();
            outputGobbler.start();

            // 等待转换完成（0表示成功）
//            int exitCode = process.waitFor();
//            System.out.println("FFmpeg 退出码：" + exitCode);
//            return exitCode == 0;
            boolean completed = process.waitFor(2, TimeUnit.MINUTES);
            if (!completed) {
                process.destroy();
                System.err.println("FFmpeg 执行超时！");
                return false;
            }
            return completed;
        } catch (Exception e) {
            System.err.println("转换过程中发生异常：");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 用于异步读取进程输出，避免缓冲区阻塞。
     */
    private static class StreamGobbler extends Thread {
        private final java.io.InputStream inputStream;
        private final String streamType;

        public StreamGobbler(java.io.InputStream inputStream, String streamType) {
            this.inputStream = inputStream;
            this.streamType = streamType;
        }

        @Override
        public void run() {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[" + streamType + "] " + line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 统计M3U8对应的TS切片数量
     *
     * @param m3u8FilePath M3U8文件完整路径   @code{ D:\mp4\hls\output.m3u8}
     * @return TS切片数量
     */
    public static int countTsFiles(String m3u8FilePath) {
        File m3u8File = new File(m3u8FilePath);
        if (!m3u8File.exists()) {
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(m3u8File))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                // 过滤空行和注释行（以#开头），统计.ts结尾的行
                if (line.trim().endsWith(".ts") && !line.startsWith("#")) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 扫描目录统计TS文件数量
     *
     * @param outputDir 输出目录（M3U8和TS所在目录  @code{ D:\mp4\hls}）
     * @return TS切片数量
     */
    public static int countTsInDir(String outputDir) {
        File dir = new File(outputDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }

        // 过滤出所有.ts文件
        File[] tsFiles = dir.listFiles(file -> file.getName().endsWith(".ts"));
        return tsFiles == null ? 0 : tsFiles.length;
    }

    /**
     * 从M3U8文件中提取所有TS文件的完整路径
     *
     * @param m3u8Path M3U8文件的绝对路径
     * @return TS文件的完整路径列表
     */
    public static List<String> getTsFilesFromM3U8(String m3u8Path) {
        List<String> tsFiles = new ArrayList<>();
        File m3u8File = new File(m3u8Path);

        // 检查文件是否存在
        if (!m3u8File.exists()) {
            System.err.println("错误：M3U8文件不存在 - " + m3u8Path);
            return tsFiles;
        }

        // 获取M3U8文件所在目录（用于解析相对路径）
        String m3u8Dir = m3u8File.getParent();

        try (BufferedReader reader = new BufferedReader(new FileReader(m3u8File))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过注释行和空行
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                // 检查是否为TS文件
                if (line.endsWith(".ts")) {
                    // 处理绝对路径
                    if (line.startsWith("/") || line.contains(":\\") || line.contains(":/")) {
                        tsFiles.add(line);
                    }
                    // 处理相对路径
                    else {
//                        tsFiles.add(new File(m3u8Dir, line).getAbsolutePath());
                        tsFiles.add(Paths.get(m3u8Dir, line).toAbsolutePath().toString());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取M3U8文件时出错：" + e.getMessage());
        }

        return tsFiles;
    }

    public static void main(String[] args) {
        System.out.println(convertMp4ToM3u8("D:\\mp4\\01.mp4", "D:\\mp4\\hls\\output.m3u8", null));
        System.out.println(countTsFiles("D:\\mp4\\hls\\output.m3u8"));
        System.out.println(countTsInDir("D:\\mp4\\hls"));
        System.out.println(getTsFilesFromM3U8("D:\\mp4\\hls\\output.m3u8"));
    }
}
