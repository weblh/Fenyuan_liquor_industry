package com.fenyuan.liquor.modules.business.productstructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_product_structure")
public class BizProductStructure {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private BigDecimal quantity;
    private BigDecimal ratio;
    private String customerSource;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
    private String remark;
}