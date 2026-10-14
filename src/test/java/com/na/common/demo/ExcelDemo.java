package com.na.common.demo;

import com.na.common.utils.ExcelExportResultUtil;
import com.na.common.utils.ExcelImportResultUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Excel 导入导出的可运行示例。
 *
 * <p>原先这两个示例分别以 {@code main} 方法住在 {@code ExcelExportResultUtil} 与
 * {@code ExcelImportResultUtil} 里，导致生产代码反向依赖 {@code demo} 包，
 * 并且会在工作目录留下 {@code 用户导出.xlsx}。示例属于测试源集，故移到此处：
 * 生产 jar 不再包含自测入口与 {@code System.out} 输出，示例本身仍可运行。
 *
 * <p>运行方式（工作目录为模块根）：
 * <pre>
 * mvn -o -q test-compile
 * java -cp "target/test-classes;target/classes;$(cat target/cp.txt)" com.na.common.demo.ExcelDemo
 * </pre>
 */
public final class ExcelDemo {

    private ExcelDemo() {
    }

    /**
     * 演示导出：把两个示例对象写成 xlsx
     *
     * @param args 未使用
     * @throws Exception 写出失败
     */
    public static void main(String[] args) throws Exception {
        exportDemo();
        System.out.println("导入示例见 ExcelDemo.parseUploadedFile(MultipartFile)。");
    }

    /**
     * 导出示例。
     * <p>{@code UserExport} 上用 {@code @ExcelExportColumn} 声明了表头名、顺序与日期格式
     *
     * @throws Exception 写出失败
     */
    private static void exportDemo() throws Exception {
        UserExport u1 = new UserExport(1L, "张三", new Date(), LocalDateTime.now());
        UserExport u2 = new UserExport(2L, "李四", new Date(), LocalDateTime.now().minusDays(1));

        try (FileOutputStream fos = new FileOutputStream("用户导出.xlsx")) {
            ExcelExportResultUtil.export(Arrays.asList(u1, u2), UserExport.class, fos, 15);
        }
        System.out.println("导出完成: 用户导出.xlsx");
    }

    /**
     * 按上传文件解析 Excel，返回成功数据与逐行错误信息。
     * <p>这是 {@code ExcelImportResultUtil} 的标准用法，供宿主在自己的
     * Controller 里调用（示例本身没有可用的上传文件，故不在 main 里执行）。
     *
     * @param file 上传的 xlsx/xls 文件
     * @return 解析结果；两个出口分别是成功数据与逐行错误信息
     */
    public static ExcelImportResultUtil<CityImport> parseUploadedFile(MultipartFile file) {
        ExcelImportResultUtil<CityImport> result = ExcelImportResultUtil.getList(CityImport.class, file);
        List<CityImport> data = result.getData();
        List<String> errors = result.getErrorMessages();
        if (!errors.isEmpty()) {
            System.out.println("导入存在失败行: " + errors);
        }
        // 成功导入的处理，比如入库
        data.forEach(System.out::println);
        return result;
    }
}
