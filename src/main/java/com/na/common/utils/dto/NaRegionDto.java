package com.na.common.utils.dto;

import lombok.Data;

import java.util.List;

@Data
public class NaRegionDto {
    public String code;
    public String name;
    public List<NaRegionDto> children;

    // 加一个父code，方便逆向查找路径
    public String parentCode;

    @Override
    public String toString() {
        return name + "(" + code + ")";
    }
}
