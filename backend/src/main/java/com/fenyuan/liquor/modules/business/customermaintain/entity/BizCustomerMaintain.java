package com.fenyuan.liquor.modules.business.customermaintain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_customer_maintain")
public class BizCustomerMaintain {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerName;
    private String contactPhone;
    /** yyyy-MM-dd；无购买记录时为 null */
    @TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private String lastPurchaseDate;
    /** 未复购天数；无购买记录时为 null（避免落库成默认 0） */
    @TableField(insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private Integer daysSincePurchase;
    /** 0正常 1-60天未复购 */
    private Integer alertStatus;
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
