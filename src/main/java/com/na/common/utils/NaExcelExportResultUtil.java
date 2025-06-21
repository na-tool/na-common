package com.na.common.utils;

import com.na.common.annotation.NaExcelExportColumn;
import com.na.common.demo.UserExport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
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

public class NaExcelExportResultUtil {
    /**
     * @param templatePath 模板路径，如 "/templates/template.xlsx"
     * @param dataList     要填充的数据（每行一个 Object[]）
     * @param outputStream 输出流
     */
    public static void fillTemplateAndExport(String templatePath, List<Object[]> dataList, OutputStream outputStream) {
        try (InputStream is = NaExcelExportResultUtil.class.getResourceAsStream(templatePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            int startRow = 1; // 从第2行开始写数据（0-based）
            for (int i = 0; i < dataList.size(); i++) {
                Object[] rowData = dataList.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) row = sheet.createRow(startRow + i);

                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j, CellType.STRING);
                    Object value = rowData[j];
                    cell.setCellValue(value == null ? "" : value.toString());
                }
            }

            workbook.write(outputStream);

        } catch (IOException e) {
            throw new RuntimeException("模板导出失败：" + e.getMessage(), e);
        }
    }
//    public static void main(String[] args) throws Exception {
//        List<Object[]> data = Arrays.asList(
//                new Object[]{"张三", "13800138000", "北京市海淀区", "北京", "聚贤楼"},
//                new Object[]{"李四", "13900139000", "上海市浦东新区", "上海", "老饭店"}
//        );
//
//        try (FileOutputStream fos = new FileOutputStream("填充模板导出.xlsx")) {
//            fillTemplateAndExport("/templates/template.xlsx", data, fos);
//        }
//    }

    /**
     * 导出方法
     *
     * @param <T> 数据类型
     * @param dataList 导出的数据列表
     * @param clazz 数据类型的类对象
     * @param out 输出流
     * @param minColWidth 最小列宽
     * @throws Exception 如果导出过程中出现任何异常
     */
    public static <T> void export(List<T> dataList, Class<T> clazz, OutputStream out, int minColWidth) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("导出数据");

        // 读取带注解字段并排序
        List<Field> excelFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(NaExcelExportColumn.class)) {
                System.out.println("导出字段：" + field.getName());
                excelFields.add(field);
            }
        }
        if (excelFields.isEmpty()) {
            System.out.println("没有找到导出字段，请检查注解是否正确");
        }
        excelFields.sort(Comparator.comparingInt(f -> f.getAnnotation(NaExcelExportColumn.class).order()));

        // 创建标题样式（加粗+居中）
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(false);
        // 边框
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 创建数据样式（居中）
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setWrapText(false);
        // 边框
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 写表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < excelFields.size(); i++) {
            NaExcelExportColumn col = excelFields.get(i).getAnnotation(NaExcelExportColumn.class);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(col.headerName());
            cell.setCellStyle(headerStyle);
        }

        // 写数据
        int rowIndex = 1;
        for (T item : dataList) {
            System.out.println("导出对象：" + item);
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < excelFields.size(); i++) {
                Field field = excelFields.get(i);
                field.setAccessible(true);
                Object value = field.get(item);
                Cell cell = row.createCell(i);
                if (value == null) {
                    cell.setCellValue("");
                } else {
                    NaExcelExportColumn col = field.getAnnotation(NaExcelExportColumn.class);
                    String fmt = col.dateFormat();
                    if (!fmt.isEmpty()) {
                        if (value instanceof Date) {
                            String formatted = new SimpleDateFormat(fmt).format((Date) value);
                            cell.setCellValue(formatted);
                        } else if (value instanceof LocalDateTime) {
                            String formatted = ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(fmt));
                            cell.setCellValue(formatted);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
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

    // 重载简化方法，默认最小列宽为0
    private static <T> void export(List<T> dataList, Class<T> clazz, OutputStream out) throws Exception {
        export(dataList, clazz, out, 0);
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

    /**
     * 将 Excel 导出到 HTTP 响应流
     * @param response HTTP 响应对象
     * @param dataList 导出的数据列表
     * @param clazz 数据类型的类对象
     * @param minColWidth 最小列宽
     * @param fileName 导出文件名
     * @param <T> 数据类型
     */
    public static <T> void download(HttpServletResponse response,
                                    List<T> dataList,
                                    Class<T> clazz,
                                    int minColWidth,
                                    String fileName) {
        try (OutputStream out = response.getOutputStream()) {
            // 设置响应头，避免中文乱码
            String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            export(dataList, clazz, out, minColWidth);
            out.flush();
        } catch (Exception e) {
            System.out.println("导出失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将 Excel 导出到本地文件
     *
     * @param <T> 数据类型
     * @param dataList 导出的数据列表
     * @param clazz 数据类型的类对象
     * @param minColWidth 最小列宽
     * @param filePath 本地文件路径
     */
    public static <T> void exportToLocalFile(List<T> dataList, Class<T> clazz, int minColWidth, String filePath ) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            export(dataList, clazz, fos, minColWidth);
            System.out.println("导出成功：" + filePath);
        } catch (Exception e) {
            System.out.println("导出本地文件失败：" + e.getMessage());
            e.printStackTrace();
        }
    }


}
