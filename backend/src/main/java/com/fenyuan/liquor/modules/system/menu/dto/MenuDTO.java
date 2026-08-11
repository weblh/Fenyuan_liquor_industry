package com.fenyuan.liquor.modules.system.menu.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class MenuDTO {
    private Long id;
    private Long parentId;
    @NotBlank(message = "菜单名称不能为空")
    private String name;
    private String path;
    private String component;
    private String permission;
    @NotNull(message = "菜单类型不能为空")
    private Integer type;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer keepAlive;
    private Integer status;
    private String remark;
}
