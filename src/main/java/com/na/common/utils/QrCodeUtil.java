package com.na.common.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * 二维码生成工具类(基于 ZXing): 支持生成 PNG 字节、Base64 字符串, 可自定义颜色、嵌入 Logo
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 1. 生成 Base64 二维码(不含 data URI 前缀, 可直接存 DB 或返回给前端拼接 <img src="data:image/png;base64,xxx">)
 * String base64 = QrCodeUtil.createQRCodeBase64("https://example.com", 300, 300);
 *
 * // 2. 生成 PNG 字节数组(用于写入本地文件或 Servlet 响应流)
 * byte[] pngBytes = QrCodeUtil.createQRCodeBytes("https://example.com", 300, 300);
 * Files.write(Paths.get("qr.png"), pngBytes);
 *
 * // 3. 生成带 Logo + 自定义颜色的二维码
 * String base64WithLogo = QrCodeUtil.createQRCodeWithLogo(
 *         "https://example.com", 400, 400,
 *         "https://cdn.example.com/logo.png",
 *         0xFF1E90FF,  // 道奇蓝
 *         0xFFFFFFFF); // 白背景
 * }</pre>
 */
@Slf4j
public final class QrCodeUtil {

    /** 默认二维码容错级别(H = 最高约 30% 可恢复, 适合嵌入 Logo) */
    private static final ErrorCorrectionLevel DEFAULT_ECC = ErrorCorrectionLevel.H;

    /** 默认边距(单位: 模块) */
    private static final int DEFAULT_MARGIN = 2;

    /** 无边距(仅用于带 Logo 模式, 1 个模块) */
    private static final int MIN_MARGIN_FOR_LOGO = 1;

    /** Logo 默认占二维码总宽高的比例(1/5 = 20%, 约等于容错率余量上限, 不影响识别) */
    private static final int LOGO_DIVISOR = 5;

    /** 远程 Logo 读取连接超时(毫秒) */
    private static final int LOGO_CONNECT_TIMEOUT_MS = 5_000;

    /** 远程 Logo 读取读取超时(毫秒) */
    private static final int LOGO_READ_TIMEOUT_MS = 10_000;

    /** 默认二维码前景色(黑) */
    static final int DEFAULT_ON_COLOR = MatrixToImageConfig.BLACK;

    /** 默认二维码背景色(白) */
    static final int DEFAULT_OFF_COLOR = MatrixToImageConfig.WHITE;

    /** 纯静态工具类, 禁止实例化 */
    private QrCodeUtil() {
    }

    /**
     * 生成二维码并返回 Base64 字符串（不包含 {@code data:image/png;base64,} 前缀）
     *
     * @param content 二维码内容; 为 null/空白时返回 null
     * @param width   二维码像素宽度; <=0 时返回 null
     * @param height  二维码像素高度; <=0 时返回 null
     * @return Base64 编码后的 PNG 图像字符串; 参数非法时返回 null
     * @throws IOException 图像写出或编码处理失败时抛出
     */
    public static String createQRCodeBase64(String content, int width, int height) throws IOException {
        if (!isValidContent(content) || !isValidSize(width, height)) {
            return null;
        }
        return Base64.getEncoder().encodeToString(createQRCodeBytesInternal(content, width, height));
    }

    /**
     * 生成二维码并返回 PNG 格式字节数组
     *
     * @param content 二维码内容; 为 null/空白时返回 null
     * @param width   二维码像素宽度; <=0 时返回 null
     * @param height  二维码像素高度; <=0 时返回 null
     * @return PNG 图像字节数组; 参数非法时返回 null
     * @throws IOException 图像写出或编码处理失败时抛出
     */
    public static byte[] createQRCodeBytes(String content, int width, int height) throws IOException {
        if (!isValidContent(content) || !isValidSize(width, height)) {
            return null;
        }
        return createQRCodeBytesInternal(content, width, height);
    }

    /**
     * 生成嵌入 Logo 的二维码（返回 Base64, 不含 data URI 前缀）
     * <p>
     * <b>注意:</b> 该方法强制使用 {@link ErrorCorrectionLevel#H} 容错(约 30% 可恢复), 确保 Logo 不影响扫码识别;
     * logoPath 支持本地绝对路径或 http/https URL
     *
     * @param content  二维码内容; 为 null/空白时返回 null
     * @param width    二维码像素宽度; <=0 时返回 null
     * @param height   二维码像素高度; <=0 时返回 null
     * @param logoPath logo 图片路径(本地文件路径或 http/https URL); 为 null/空白时退化为无 Logo 版本
     * @param onColor  二维码前景(点)色, ARGB 格式如 {@code 0xFF1E90FF}; 为 null 时使用黑色
     * @param offColor 二维码背景色, ARGB 格式如 {@code 0xFFFFFFFF}; 为 null 时使用白色
     * @return Base64 编码后的 PNG 图像字符串; 参数非法时返回 null
     * @throws IOException 生成二维码或读取 Logo 时出错抛出
     */
    public static String createQRCodeWithLogo(String content, int width, int height, String logoPath,
                                              Integer onColor, Integer offColor) throws IOException {
        if (!isValidContent(content) || !isValidSize(width, height)) {
            return null;
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BufferedImage qrImage = generateQRCodeImage(content, width, height, onColor, offColor);

            // logoPath 为空时不嵌入 Logo, 直接输出
            if (StringUtils.hasText(logoPath)) {
                BufferedImage logo = readLogo(logoPath);
                if (logo != null) {
                    insertLogo(qrImage, logo);
                } else {
                    log.warn("Logo 读取失败, 已忽略嵌入: {}", logoPath);
                }
            }

            ImageIO.write(qrImage, "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (WriterException e) {
            throw new IOException("生成带Logo二维码失败", e);
        }
    }

    // ========== 内部辅助方法 ==========

    /**
     * 共享的二维码矩阵生成逻辑(内部复用, 不重复写 hints)
     *
     * @param content 内容
     * @param width   宽
     * @param height  高
     * @param margin  边距(模块数)
     * @param ecc     容错级别
     * @return 位矩阵
     */
    private static BitMatrix encodeBitMatrix(String content, int width, int height,
                                              int margin, ErrorCorrectionLevel ecc) throws WriterException {
        // EnumMap 比 HashMap 对于枚举键更紧凑、更快
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ecc);
        hints.put(EncodeHintType.MARGIN, margin);
        return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    }

    /**
     * 生成 PNG 字节(内部共享, 参数已校验)
     */
    private static byte[] createQRCodeBytesInternal(String content, int width, int height) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BitMatrix bitMatrix = encodeBitMatrix(content, width, height, DEFAULT_MARGIN, DEFAULT_ECC);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ImageIO.write(image, "png", os);
            return os.toByteArray();
        } catch (WriterException e) {
            throw new IOException("二维码生成失败", e);
        }
    }

    /**
     * 生成带可选颜色的二维码图像(用于带 Logo 场景)
     *
     * @param content  内容
     * @param width    宽
     * @param height   高
     * @param onColor  前景色; null=默认黑
     * @param offColor 背景色; null=默认白
     */
    private static BufferedImage generateQRCodeImage(String content, int width, int height,
                                                     Integer onColor, Integer offColor) throws WriterException {
        BitMatrix bitMatrix = encodeBitMatrix(content, width, height, MIN_MARGIN_FOR_LOGO, DEFAULT_ECC);
        // 颜色为 null 时回落 ZXing 默认黑白, 避免调用方传半参数(null + 非 null)导致异常
        int on = onColor != null ? onColor : DEFAULT_ON_COLOR;
        int off = offColor != null ? offColor : DEFAULT_OFF_COLOR;
        return MatrixToImageWriter.toBufferedImage(bitMatrix, new MatrixToImageConfig(on, off));
    }

    /**
     * 读取 Logo 图片(本地文件或 http/https URL), 远程读取内置超时, 避免挂死
     *
     * @param logoPath 本地路径或 http/https URL
     * @return Logo 图像; 读取失败返回 null
     */
    private static BufferedImage readLogo(String logoPath) {
        try {
            // 使用 CommonUtil.ishttp 判断是否远程地址(该方法在 na-common 中已存在)
            if (CommonUtil.ishttp(logoPath)) {
                HttpURLConnection conn = (HttpURLConnection) new URL(logoPath).openConnection();
                conn.setConnectTimeout(LOGO_CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(LOGO_READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                try (InputStream in = conn.getInputStream()) {
                    // 先把网络流读入内存, 避免 ImageIO 在慢速网络上反复 seek
                    byte[] bytes = StreamUtils.copyToByteArray(in);
                    return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                } finally {
                    conn.disconnect();
                }
            }
            return ImageIO.read(new File(logoPath));
        } catch (Exception e) {
            log.error("读取Logo失败: {}", logoPath, e);
            return null;
        }
    }

    /**
     * 将 Logo 嵌入二维码图像中央(带圆角白色边框)
     *
     * @param qrImage 二维码图像(会被原地修改)
     * @param logo    Logo 图像; 为 null 时跳过
     */
    private static void insertLogo(BufferedImage qrImage, BufferedImage logo) {
        if (logo == null) {
            return;
        }
        Graphics2D g = qrImage.createGraphics();
        try {
            // 抗锯齿 + 高质量插值, 使 Logo 缩放后不糊
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int qrWidth = qrImage.getWidth();
            int qrHeight = qrImage.getHeight();
            int logoWidth = Math.max(1, qrWidth / LOGO_DIVISOR);
            int logoHeight = Math.max(1, qrHeight / LOGO_DIVISOR);
            int x = (qrWidth - logoWidth) / 2;
            int y = (qrHeight - logoHeight) / 2;

            // 先画 Logo
            g.drawImage(logo, x, y, logoWidth, logoHeight, null);
            // 再画白底圆角描边(2px 宽, 圆角 10px), 让 Logo 与二维码过渡不突兀
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, logoWidth, logoHeight, 10, 10);
        } finally {
            g.dispose();
        }
    }

    /**
     * 校验 content 是否为有效内容(null/空白返回 false)
     */
    private static boolean isValidContent(String content) {
        return StringUtils.hasText(content);
    }

    /**
     * 校验宽高是否合法(>0 才合法)
     */
    private static boolean isValidSize(int width, int height) {
        return width > 0 && height > 0;
    }
}
