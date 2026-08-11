package com.fenyuan.liquor.modules.auth.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Long deptId;
    private Integer isAdmin;
    private Set<String> roles;
    private Set<String> permissions;
}
