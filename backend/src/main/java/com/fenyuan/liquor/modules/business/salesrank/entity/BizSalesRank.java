package com.fenyuan.liquor.modules.business.salesrank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_sales_rank")
public class BizSalesRank {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String companyName;
    private BigDecimal amount;
    private BigDecimal salesRatio;
    private Integer trend;
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