package com.fenyuan.liquor.modules.system.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer sex;
    private Long deptId;
    private Integer status;
    private Integer isAdmin;
    private String remark;
    private List<Long> roleIds;
}
