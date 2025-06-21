package com.na.common.utils;

import com.na.common.annotation.NaExcelImportColumn;
import com.na.common.demo.CityImport;
import lombok.Data;
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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Data
public class NaExcelImportResultUtil<T> {
    private List<T> data = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();

    private void addError(int rowIndex, String message) {
        errorMessages.add("第 " + (rowIndex + 1) + " 行：" + message);
    }

    /**
     * @param clazz    结果类型 例如 CityModel.class
     * @param file 要导入的 Excel 文件
     * @param <M> 结果类型
     * @return {@code NaExcelImportResultUtil<M>}
     */
    public static <M> NaExcelImportResultUtil<M> getList(Class<M> clazz, MultipartFile file) {
        NaExcelImportResultUtil<M> result = new NaExcelImportResultUtil<>();

        try (InputStream fis = file.getInputStream()) {
            Workbook workbook = file.getOriginalFilename().endsWith("xlsx") ?
                    new XSSFWorkbook(fis) : new HSSFWorkbook(fis);

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.rowIterator();
            if (rows.hasNext()) rows.next(); // skip header

            int rowIndex = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                M instance = clazz.getDeclaredConstructor().newInstance();
                boolean hasError = false;

                for (Field field : clazz.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(NaExcelImportColumn.class)) continue;
                    NaExcelImportColumn column = field.getAnnotation(NaExcelImportColumn.class);
                    String rawValue = getCellValue(row.getCell(column.index()));
                    if (StringUtils.isBlank(rawValue)) continue;

                    try {
                        Method setter = clazz.getMethod("set" + StringUtils.capitalize(field.getName()), field.getType());
                        Object value = convertValue(field.getType(), rawValue);
                        setter.invoke(instance, value);
                    } catch (Exception e) {
                        result.addError(rowIndex, "字段[" + field.getName() + "]解析失败，值[" + rawValue + "]，错误: " + e.getMessage());
                        hasError = true;
                    }
                }

                if (!hasError) result.getData().add(instance);
                rowIndex++;
            }

        } catch (IOException e) {
            result.addError(0, "文件读取失败：" + e.getMessage());
        } catch (Exception e) {
            result.addError(0, "导入异常：" + e.getMessage());
        }

        return result;
    }


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

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    return format.format(date);
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
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
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

    public static void main(String[] args) {
        MultipartFile file = null ;
        NaExcelImportResultUtil<CityImport> result = NaExcelImportResultUtil.getList(CityImport.class, file);

        List<CityImport> data = result.getData();
        List<String> errors = result.getErrorMessages();

        // 可根据情况返回解析结果
        if (!errors.isEmpty()) {
            System.out.println("导入失败！");
        }

        // 成功导入的处理，比如入库
        data.forEach(System.out::println); // 这里只是打印
    }

}
