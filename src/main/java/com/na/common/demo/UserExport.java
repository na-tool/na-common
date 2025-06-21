package com.na.common.demo;

import com.na.common.annotation.NaExcelExportColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExport {
    @NaExcelExportColumn(headerName = "用户ID", order = 1)
    private Long id;

    @NaExcelExportColumn(headerName = "用户名", order = 2)
    private String name;

    @NaExcelExportColumn(headerName = "注册时间（Date）", order = 3, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date registerTime;

    @NaExcelExportColumn(headerName = "最后登录（LocalDateTime）", order = 4, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLogin;

}
