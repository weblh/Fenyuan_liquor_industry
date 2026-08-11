package com.fenyuan.liquor.modules.system.menu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String permission;
    private Integer type;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer keepAlive;
    private Integer status;
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

    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
