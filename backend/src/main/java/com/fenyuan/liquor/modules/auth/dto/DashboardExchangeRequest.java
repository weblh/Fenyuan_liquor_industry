package com.fenyuan.liquor.modules.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DashboardExchangeRequest {

    /** 广州总大屏登录后拿到的丰驰 JWT */
    @NotBlank(message = "fengchiToken 不能为空")
    private String fengchiToken;

    /** 调用来源标识，可选 */
    private String client;
}
