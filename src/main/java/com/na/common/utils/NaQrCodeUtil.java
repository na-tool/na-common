package com.na.common.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 生成二维码相关工具方法
 */
public class NaQrCodeUtil {
    /**
     * 生成二维码并返回 Base64 字符串（不包含 data:image/png;base64 前缀）
     *
     * @param content 二维码内容
     * @param width   二维码宽度
     * @param height  二维码高度
     * @return Base64 编码后的二维码图像字符串
     * @throws IOException IO异常
     */
    public static String createQRCodeBase64(String content, int width, int height) throws IOException {
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            // 配置二维码参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);

            // 生成二维码矩阵
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // 写入输出流并转为 Base64
            ImageIO.write(image, "png", os);
            return Base64.encodeBase64String(os.toByteArray());
        } catch (WriterException e) {
            throw new IOException("二维码生成失败", e);
        } finally {
            os.close();
        }
    }

    /**
     * 生成二维码并返回字节数组（PNG 格式）
     *
     * @param content 二维码内容
     * @param width   宽度
     * @param height  高度
     * @return 图片的字节数组
     * @throws IOException IO异常
     */
    public static byte[] createQRCodeBytes(String content, int width, int height) throws IOException {
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            // 配置参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ImageIO.write(image, "png", os);
            return os.toByteArray();
        } catch (WriterException e) {
            throw new IOException("二维码生成失败", e);
        } finally {
            os.close();
        }
    }

    /**
     * 生成嵌入 logo 的二维码（返回 Base64）
     *
     * @param content 二维码内容
     * @param width   宽度
     * @param height  高度
     * @param logoPath logo 图片路径 （支持本地绝对路径）
     * @return 生成后的二维码 Base64 字符串 （不含 data:image/png;base64 前缀）
     * @throws IOException 生成二维码或读取 logo 文件时出错
     */
    public static String createQRCodeWithLogo(String content, int width, int height, String logoPath) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BufferedImage qrImage = generateQRCodeImage(content, width, height);
            BufferedImage logo = ImageIO.read(new File(logoPath));

            // 将 logo 嵌入二维码
            insertLogo(qrImage, logo);

            // 输出为 PNG 并转 Base64
            ImageIO.write(qrImage, "png", os);
            return java.util.Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) {
            throw new IOException("生成带Logo二维码失败", e);
        }
    }

    // 生成二维码图像
    private static BufferedImage generateQRCodeImage(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H 级别容错，适合嵌入 logo
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix, new MatrixToImageConfig());
    }

    // 将 logo 嵌入二维码图像中央
    private static void insertLogo(BufferedImage qrImage, BufferedImage logo) {
        Graphics2D g = qrImage.createGraphics();

        // 计算 logo 位置和尺寸（宽高占比约为二维码的1/5）
        int logoWidth = qrImage.getWidth() / 5;
        int logoHeight = qrImage.getHeight() / 5;
        int x = (qrImage.getWidth() - logoWidth) / 2;
        int y = (qrImage.getHeight() - logoHeight) / 2;

        // 绘制 logo
        g.drawImage(logo, x, y, logoWidth, logoHeight, null);
        g.setStroke(new BasicStroke(2));
        g.setColor(Color.WHITE); // 白色边框
        g.drawRoundRect(x, y, logoWidth, logoHeight, 10, 10);
        g.dispose();
    }
}
