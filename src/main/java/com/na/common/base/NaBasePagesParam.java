package com.na.common.base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@ApiModel
@Data
public class NaBasePagesParam {
    @ApiModelProperty(value = "当前页码")
    private Integer currentPage = 1;
    @ApiModelProperty(value = "每页条数")
    private Integer pageSize = 10;
    @ApiModelProperty(value = "模糊查询（按需使用）")
    private String searchVal;

}