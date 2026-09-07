package com.fenyuan.liquor.modules.finance.kingdeevoucher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_kingdee_voucher")
public class FinKingdeeVoucher {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bankFlowNo;
    private String counterparty;
    private BigDecimal amount;
    /** yyyy-MM-dd */
    private String flowDate;
    /** 0待同步 1已写入 2失败 */
    private Integer syncStatus;
    private String voucherNo;
    /** yyyy-MM-dd HH:mm:ss */
    private String syncTime;
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
