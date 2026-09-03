package com.na.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "分页查询参数")
@Data
public class BasePagesParam {
    @Schema(description = "当前页码", example = "1")
    private Integer currentPage = 1;
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
    @Schema(description = "模糊查询（按需使用）")
    private String searchVal;

}