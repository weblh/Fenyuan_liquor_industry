package com.fenyuan.liquor.modules.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserInfoVO userInfo;
}
