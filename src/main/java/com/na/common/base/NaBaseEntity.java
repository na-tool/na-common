package com.na.common.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("所有实体类的超类")
public class NaBaseEntity<T> implements Serializable {
    private static final long serialVersionUID = 3232891580617586888L;

    @ApiModelProperty("唯一主键id")
    @TableId(type = IdType.INPUT)
    private String id;

}
