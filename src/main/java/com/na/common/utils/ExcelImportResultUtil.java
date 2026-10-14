package com.na.common.utils;

import com.na.common.annotation.ExcelImportColumn;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Excel 导入工具类(基于注解 {@link ExcelImportColumn})
 * <p>
 * 读取 xlsx/xls 文件, 按注解映射列到字段, 支持 14 种常见类型自动转换
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 实体类标注注解
 * public class CityImport {
 *     @ExcelImportColumn(index = 0)
 *     private String name;
 *     @ExcelImportColumn(index = 1)
 *     private Integer population;
 * }
 *
 * // 导入
 * ExcelImportResultUtil<CityImport> result = ExcelImportResultUtil.getList(CityImport.class, file);
 * List<CityImport> data = result.getData();       // 成功解析的数据
 * List<String> errors = result.getErrorMessages(); // 失败行的错误信息
 * }</pre>
 */
@Slf4j
@Data
public class ExcelImportResultUtil<T> {

    /** 成功解析的数据 */
    private List<T> data = new ArrayList<>();

    /** 错误信息(行号 + 原因) */
    private List<String> errorMessages = new ArrayList<>();

    /**
     * 添加错误信息
     *
     * @param rowIndex 行索引(0-based, 显示时 +1 转为 1-based)
     * @param message  错误描述
     */
    private void addError(int rowIndex, String message) {
        errorMessages.add("第 " + (rowIndex + 1) + " 行: " + message);
    }

    /**
     * 从 Excel 文件解析数据
     *
     * @param clazz 结果类型, 需有无参构造器和对应字段的 setter
     * @param file  上传的 Excel 文件(xlsx 或 xls)
     * @param <M>   结果类型
     * @return 解析结果(含成功数据和错误信息)
     */
    public static <M> ExcelImportResultUtil<M> getList(Class<M> clazz, MultipartFile file) {
        ExcelImportResultUtil<M> result = new ExcelImportResultUtil<>();

        // Workbook 也必须纳入 try-with-resources: 否则 POI 占用的内存与临时文件要等 GC 才释放
        try (InputStream fis = file.getInputStream();
             Workbook workbook = resolveWorkbook(file, fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.rowIterator();

            // 跳过表头
            if (rows.hasNext()) {
                rows.next();
            }

            int rowIndex = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                M instance = clazz.getDeclaredConstructor().newInstance();
                boolean hasError = false;

                for (Field field : clazz.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(ExcelImportColumn.class)) {
                        continue;
                    }
                    ExcelImportColumn column = field.getAnnotation(ExcelImportColumn.class);
                    String rawValue = getCellValue(row.getCell(column.index()));
                    if (StringUtils.isBlank(rawValue)) {
                        continue;
                    }

                    try {
                        Method setter = clazz.getMethod(
                                "set" + StringUtils.capitalize(field.getName()), field.getType());
                        Object value = convertValue(field.getType(), rawValue);
                        setter.invoke(instance, value);
                    } catch (Exception e) {
                        result.addError(rowIndex, "字段[" + field.getName()
                                + "]解析失败, 值[" + rawValue + "], 错误: " + e.getMessage());
                        hasError = true;
                    }
                }

                if (!hasError) {
                    result.getData().add(instance);
                }
                rowIndex++;
            }

        } catch (IOException e) {
            result.addError(0, "文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            result.addError(0, "导入异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 根据文件扩展名选择 Workbook 实现
     *
     * @param file 上传文件
     * @param fis  输入流
     * @return Workbook 实例
     * @throws IOException 文件读取失败
     */
    private static Workbook resolveWorkbook(MultipartFile file, InputStream fis) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        }
        return new HSSFWorkbook(fis);
    }

    /**
     * 将字符串值转换为目标类型
     * <p>
     * 支持: String, Integer, Long, Double, Float, Boolean, BigDecimal, BigInteger, Date, LocalDateTime
     *
     * @param type  目标类型
     * @param value 字符串值
     * @return 转换后的值
     * @throws Exception 类型不匹配或解析失败
     */
    private static Object convertValue(Class<?> type, String value) throws Exception {
        if (type == String.class) return value;
        if (type == Integer.class) return Integer.valueOf(value);
        if (type == Long.class) return Long.valueOf(value);
        if (type == Double.class) return Double.valueOf(value);
        if (type == Float.class) return Float.valueOf(value);
        if (type == Boolean.class) return Boolean.valueOf(value);
        if (type == BigDecimal.class) return new BigDecimal(value);
        if (type == BigInteger.class) return new BigInteger(value);
        if (type == Date.class) return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(value);
        if (type == LocalDateTime.class)
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        return null;
    }

    /**
     * 读取单元格值并转为字符串
     * <p>
     * 处理 NUMERIC(含日期)/STRING/BOOLEAN/FORMULA/BLANK/ERROR 六种类型
     *
     * @param cell 单元格, 可能为 null
     * @return 字符串值; null 单元格返回空串
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date);
                } else {
                    DecimalFormat df = new DecimalFormat("####.####");
                    return df.format(cell.getNumericCellValue());
                }
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
                            .getCreationHelper().createFormulaEvaluator();
                    evaluator.evaluateFormulaCell(cell);
                    return getCellValue(cell);
                }
            case BLANK:
                return "";
            case ERROR:
                return "非法字符";
            default:
                return "未知类型";
        }
    }

}
