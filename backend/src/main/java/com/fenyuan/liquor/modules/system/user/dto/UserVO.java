package com.fenyuan.liquor.modules.system.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer sex;
    private Long deptId;
    private Integer status;
    private Integer isAdmin;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime createTime;
    private String remark;
    private List<Long> roleIds;
}
