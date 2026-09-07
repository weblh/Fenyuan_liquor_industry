package com.fenyuan.liquor.modules.business.offsitesale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_offsite_sale")
public class BizOffsiteSale {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productName;
    private BigDecimal quantity;
    private String province;
    private String city;
    private String address;
    /** ERP / JD */
    private String sourceType;
    private String orderNo;
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
