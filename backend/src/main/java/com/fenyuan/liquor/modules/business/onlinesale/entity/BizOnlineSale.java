package com.fenyuan.liquor.modules.business.onlinesale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_online_sale")
public class BizOnlineSale {

    @TableId(type = IdType.AUTO)
    private Long id;
    private BigDecimal saleAmount;
    private BigDecimal shipAmount;
    private BigDecimal paymentAmount;
    private String periodName;
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