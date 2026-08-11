package com.fenyuan.liquor.modules.system.role.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class RoleDTO {
    private Long id;
    @NotBlank(message = "角色名称不能为空")
    private String name;
    @NotBlank(message = "角色编码不能为空")
    private String code;
    private String description;
    private Integer sort;
    private Integer status;
    private Integer dataScope;
    private String remark;
    private List<Long> menuIds;
}
