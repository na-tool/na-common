package com.na.common.utils;

import com.na.common.annotation.ExcelExportColumn;
import com.na.common.demo.UserExport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
        Workbook workbook = new XSSFWorkbook();
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
        workbook.close();
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
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ExcelExportColumn.class)) {
                fields.add(field);
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

    public static void main(String[] args) throws Exception {
        UserExport u1 = new UserExport(1L, "张三", new Date(), LocalDateTime.now());
        UserExport u2 = new UserExport(2L, "李四", new Date(), LocalDateTime.now().minusDays(1));

        try (FileOutputStream fos = new FileOutputStream("用户导出.xlsx")) {
            export(Arrays.asList(u1, u2), UserExport.class, fos, 15);
        }



//        downPipline().then((response) => {
//                const blob = new Blob([response], {
//                type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
//                });
//
//                const disposition = response?.headers?.['content-disposition'];
//        let filename = '导出数据.xlsx';
//        if (disposition) {
//                    const filenameMatch = disposition.match(/filename\*=UTF-8''(.+)$/);
//            if (filenameMatch && filenameMatch[1]) {
//                filename = decodeURIComponent(filenameMatch[1]);
//            } else {
//                        const fallbackMatch = disposition.match(/filename="(.+)"/);
//                if (fallbackMatch && fallbackMatch[1]) {
//                    filename = fallbackMatch[1];
//                }
//            }
//        }
//
//                const link = document.createElement('a');
//                const url = window.URL.createObjectURL(blob);
//        link.href = url;
//        link.setAttribute('download', filename);
//        document.body.appendChild(link);
//        link.click();
//        document.body.removeChild(link);
//        window.URL.revokeObjectURL(url);
//            }).catch(err => {
//                console.error('下载失败:', err);
//            });
    }
}
