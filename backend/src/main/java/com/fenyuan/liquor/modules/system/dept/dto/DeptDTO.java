package com.fenyuan.liquor.modules.system.dept.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeptDTO {
    private Long id;
    private Long parentId;
    @NotBlank(message = "部门名称不能为空")
    private String name;
    private String code;
    private String leader;
    private String phone;
    private String email;
    private Integer sort;
    private Integer status;
    private String remark;
}
