package com.fenyuan.liquor.modules.system.dept.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String leader;
    private String phone;
    private String email;
    private Integer sort;
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
    private List<SysDept> children = new ArrayList<>();
}
