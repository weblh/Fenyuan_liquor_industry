package com.fenyuan.liquor.modules.system.role.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer sort;
    private Integer status;
    private Integer dataScope;
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
