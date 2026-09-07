package com.fenyuan.liquor.modules.business.pricecompare.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_price_compare")
public class BizPriceCompare {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productName;
    private String spec;
    private BigDecimal salePrice;
    private BigDecimal jdPrice;
    private BigDecimal tmallPrice;
    /** yyyy-MM-dd HH:mm:ss */
    private String crawlTime;
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
