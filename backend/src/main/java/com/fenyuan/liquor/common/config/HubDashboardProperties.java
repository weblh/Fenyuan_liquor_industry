package com.fenyuan.liquor.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "hub.dashboard")
public class HubDashboardProperties {

    /** 是否启用总大屏令牌换发 */
    private boolean enabled = true;

    /** 校验丰驰 JWT 的用户信息接口 */
    private String fengchiValidateUrl = "http://127.0.0.1:8081/api/auth/current-user";

    /** 换发后使用的本系统展示账号 */
    private String displayUsername = "admin";

    private List<String> allowedEmails = new ArrayList<>();

    private List<String> allowedPhones = new ArrayList<>();
}
