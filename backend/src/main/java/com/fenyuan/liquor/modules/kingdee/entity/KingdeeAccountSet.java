package com.fenyuan.liquor.modules.kingdee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kingdee_account_set")
public class KingdeeAccountSet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long credentialId;

    private String accountName;

    private String dbId;

    private String orgCompanyCode;

    private String useOrgCode;

    private String defaultFormId;

    private String defaultAccountingPeriod;

    private String bankAccountNo;

    private Integer isDefault;

    private Integer status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String credentialName;
}
