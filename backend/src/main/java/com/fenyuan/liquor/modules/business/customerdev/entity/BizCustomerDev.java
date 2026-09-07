package com.fenyuan.liquor.modules.business.customerdev.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_customer_dev")
public class BizCustomerDev {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String openMonth;
    /** 1=开发成功（页面仅展示成功客户） */
    private Integer status;
    private BigDecimal amount;
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