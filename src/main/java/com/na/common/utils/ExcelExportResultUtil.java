package com.na.common.utils;

import com.alibaba.fastjson2.JSON;
import com.na.common.annotation.ExcelExportColumn;
import com.na.common.result.Result;
import com.na.common.result.enums.Status;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel 导出工具类(基于注解 {@link ExcelExportColumn})
 * <p>
 * 支持模板填充和注解驱动两种导出方式, 可输出到 HTTP 响应流或本地文件
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 实体类标注注解
 * public class UserExport {
 *     @ExcelExportColumn(headerName = "ID", order = 1)
 *     private Long id;
 *     @ExcelExportColumn(headerName = "姓名", order = 2)
 *     private String name;
 *     @ExcelExportColumn(headerName = "注册时间", order = 3, dateFormat = "yyyy-MM-dd")
 *     private Date createTime;
 * }
 *
 * // HTTP 导出
 * ExcelExportResultUtil.download(response, userList, UserExport.class, 15, "用户列表");
 *
 * // 本地文件导出
 * ExcelExportResultUtil.exportToLocalFile(userList, UserExport.class, 15, "D:/用户列表.xlsx");
 * }</pre>
 */
@Slf4j
public final class ExcelExportResultUtil {

    /** 纯静态工具类, 禁止实例化 */
    private ExcelExportResultUtil() {
    }

    /**
     * 按模板填充数据并导出
     * <p>
     * 从 classpath 读取 xlsx 模板, 从第 2 行(0-based index=1)开始写入数据, 保持表头不变
     *
     * @param templatePath  模板路径(classpath 相对), 如 "/templates/template.xlsx"
     * @param dataList      要填充的数据, 每行一个 Object[]
     * @param outputStream 输出流
     * @throws RuntimeException 模板读取或写入失败时抛出
     */
    public static void fillTemplateAndExport(String templatePath, List<Object[]> dataList, OutputStream outputStream) {
        try (InputStream is = ExcelExportResultUtil.class.getResourceAsStream(templatePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int startRow = 1; // 从第 2 行开始写数据(0-based), 第 1 行为表头

            for (int i = 0; i < dataList.size(); i++) {
                Object[] rowData = dataList.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) {
                    row = sheet.createRow(startRow + i);
                }
                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j, CellType.STRING);
                    Object value = rowData[j];
                    cell.setCellValue(value == null ? "" : value.toString());
                }
            }

            workbook.write(outputStream);

        } catch (IOException e) {
            throw new RuntimeException("模板导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注解驱动导出(核心方法)
     * <p>
     * 读取 {@link ExcelExportColumn} 注解字段, 按 order 排序, 生成带样式的 Excel
     *
     * @param dataList   导出的数据列表
     * @param clazz      数据类型的类对象
     * @param out        输出流
     * @param minColWidth 最小列宽(字符数), <=0 时不限制
     * @param <T>        数据类型
     * @throws Exception 导出过程中出现任何异常
     */
    public static <T> void export(List<T> dataList, Class<T> clazz, OutputStream out, int minColWidth) throws Exception {
        // try-with-resources: 中途任何异常(字段读取、写入失败等)都不会漏掉 close,
        // 原实现只在方法末尾 close, 抛异常时 POI 占用的内存与临时文件要等 GC 才释放
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导出数据");

            // 读取带注解字段并按 order 排序
            List<Field> excelFields = getAnnotatedFields(clazz);

            // 创建样式(表头加粗居中 + 数据居中, 统一加边框)
            CellStyle headerStyle = createStyle(workbook, true);
            CellStyle dataStyle = createStyle(workbook, false);

            // 写表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < excelFields.size(); i++) {
                ExcelExportColumn col = excelFields.get(i).getAnnotation(ExcelExportColumn.class);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(col.headerName());
                cell.setCellStyle(headerStyle);
            }

            // 写数据
            int rowIndex = 1;
            for (T item : dataList) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < excelFields.size(); i++) {
                    Field field = excelFields.get(i);
                    field.setAccessible(true);
                    Object value = field.get(item);
                    Cell cell = row.createCell(i);
                    setCellValue(cell, value, field.getAnnotation(ExcelExportColumn.class));
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自动列宽 + 最小列宽控制
            for (int i = 0; i < excelFields.size(); i++) {
                sheet.autoSizeColumn(i);
                if (minColWidth > 0) {
                    int currentWidth = sheet.getColumnWidth(i) / 256;
                    if (currentWidth < minColWidth) {
                        sheet.setColumnWidth(i, minColWidth * 256);
                    }
                }
            }

            workbook.write(out);
        }
    }

    /**
     * 重载简化方法, 默认最小列宽为 0
     */
    public static <T> void export(List<T> dataList, Class<T> clazz, OutputStream out) throws Exception {
        export(dataList, clazz, out, 0);
    }

    /**
     * 将 Excel 导出到 HTTP 响应流(自动设置 Content-Disposition)
     *
     * @param response    HTTP 响应对象
     * @param dataList    导出的数据列表
     * @param clazz       数据类型的类对象
     * @param minColWidth 最小列宽
     * @param fileName    导出文件名(不含 .xlsx 后缀, 自动追加)
     * @param <T>         数据类型
     */
    public static <T> void download(HttpServletResponse response,
                                    List<T> dataList,
                                    Class<T> clazz,
                                    int minColWidth,
                                    String fileName) {
        try (OutputStream out = response.getOutputStream()) {
            // 文件名 URL 编码, 兼容中文, + 替换为 %20 避免空格丢失
            String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            export(dataList, clazz, out, minColWidth);
            out.flush();
        } catch (Exception e) {
            log.error("Excel 导出失败", e);
            // 原实现只记日志就返回, 客户端收到的是「空的 200」, 无法区分导出成功与失败
            writeExportFailure(response);
        }
    }

    /**
     * 导出失败后的兜底响应: 响应尚未提交时回写 500 + 统一 JSON 错误体
     * <p>已经写出一部分内容(响应已提交)时无法再改状态码, 只能记日志
     *
     * @param response HTTP 响应对象
     */
    private static void writeExportFailure(HttpServletResponse response) {
        if (response.isCommitted()) {
            log.warn("Excel 导出失败时响应已提交, 无法回写错误状态码");
            return;
        }
        try {
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(JSON.toJSONString(
                    new Result<>(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), "导出失败，请稍后重试")));
        } catch (IOException ioException) {
            log.error("Excel 导出失败后回写错误响应也失败", ioException);
        }
    }

    /**
     * 将 Excel 导出到本地文件
     *
     * @param dataList    导出的数据列表
     * @param clazz       数据类型的类对象
     * @param minColWidth 最小列宽
     * @param filePath    本地文件路径(含 .xlsx 后缀)
     * @param <T>         数据类型
     */
    public static <T> void exportToLocalFile(List<T> dataList, Class<T> clazz, int minColWidth, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            export(dataList, clazz, fos, minColWidth);
            log.info("Excel 导出成功: {}", filePath);
        } catch (Exception e) {
            log.error("Excel 导出本地文件失败: {}", filePath, e);
        }
    }

    // === 内部辅助方法 ===

    /**
     * 获取类中标注了 {@link ExcelExportColumn} 的字段, 按 order 升序排序
     *
     * @param clazz 目标类
     * @return 排序后的字段列表
     */
    private static List<Field> getAnnotatedFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        // 沿父类链收集: 原实现只扫 clazz.getDeclaredFields(),
        // 继承自 BaseEntity 等父类的字段会被静默跳过, 既不导出也无任何提示
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(ExcelExportColumn.class)) {
                    fields.add(field);
                }
            }
        }
        fields.sort(Comparator.comparingInt(f -> f.getAnnotation(ExcelExportColumn.class).order()));
        return fields;
    }

    /**
     * 创建单元格样式
     *
     * @param workbook 工作簿
     * @param bold     true=表头样式(加粗), false=数据样式
     * @return 单元格样式
     */
    private static CellStyle createStyle(Workbook workbook, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        if (bold) {
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 设置单元格值, 支持日期格式化
     *
     * @param cell       单元格
     * @param value      值
     * @param annotation 列注解(读取 dateFormat)
     */
    private static void setCellValue(Cell cell, Object value, ExcelExportColumn annotation) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        String fmt = annotation.dateFormat();
        if (!fmt.isEmpty()) {
            if (value instanceof Date) {
                cell.setCellValue(new SimpleDateFormat(fmt).format((Date) value));
            } else if (value instanceof LocalDateTime) {
                cell.setCellValue(((LocalDateTime) value).format(DateTimeFormatter.ofPattern(fmt)));
            } else {
                cell.setCellValue(value.toString());
            }
        } else {
            cell.setCellValue(value.toString());
        }
    }

}
