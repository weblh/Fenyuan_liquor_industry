package com.fenyuan.liquor.modules.kingdee.dto;

import lombok.Data;

/**
 * 金蝶操作上下文：账套 + 登录凭证合并视图（兼容租赁项目 KingdeeAccountContext 用法）。
 */
@Data
public class KingdeeAccountContext {

    private Long id;

    private String accountName;

    private String kingdeeUrl;

    private String dbId;

    private String username;

    private String password;

    private String orgCompanyCode;

    private String useOrgCode;

    private String defaultFormId;

    private String defaultAccountingPeriod;

    private Integer isDefault;

    private Integer status;
}
