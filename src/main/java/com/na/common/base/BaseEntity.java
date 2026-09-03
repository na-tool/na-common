package com.na.common.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "所有实体类的超类")
public class BaseEntity<T> implements Serializable {
    private static final long serialVersionUID = 3232891580617586888L;

    @Schema(description = "唯一主键id")
    @TableId(type = IdType.INPUT)
    private String id;

}
