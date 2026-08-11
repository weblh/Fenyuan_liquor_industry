package com.fenyuan.liquor.modules.business.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_inventory")
public class BizInventory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productName;
    private String spec;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String warehouse;
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