package com.fenyuan.liquor.modules.settings.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField("config_key")
    private String configKey;
    @TableField("config_value")
    private String configValue;
    private String type;
    private String groupName;
    private Integer status;
    private Integer isSystem;
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
