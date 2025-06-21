package com.na.common.demo;

import com.na.common.annotation.NaExcelImportColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityImport {
    @NaExcelImportColumn(index = 0, name = "序号")
    private Integer id;

    @NaExcelImportColumn(index = 1, name = "名称")
    private String name;

    @NaExcelImportColumn(index = 2, name = "编码")
    private String code;
}
