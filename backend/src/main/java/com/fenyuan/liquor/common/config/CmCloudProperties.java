package com.fenyuan.liquor.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cmcloud")
public class CmCloudProperties {

    /** 管家婆 CMCloud 安装目录下的 Data/BaseInfo */
    private String baseInfoPath = "D:/F/CMCloud/Data/BaseInfo";

    /** 启用本地缓存同步 */
    private boolean syncEnabled = true;

    /** 财贸 API 插件地址（需 CMCloud 客户端安装 API 插件并登录账套） */
    private String apiBaseUrl = "http://127.0.0.1:45100";

    /** 销售/库存单据同步回溯天数 */
    private int billSyncDays = 365;

    /** 云 API：账套数据库名（cloud 模式必填） */
    private String dbName = "";

    /** 云 API：apiparam（cloud 模式必填） */
    private String apiParam = "";

    /** 云 API：SignKey（cloud 模式必填） */
    private String signKey = "";

    /** 云 API：AppKey（cloud 模式必填） */
    private String appKey = "";

    /** 云 API：ServerId，无则传 0 */
    private String serviceId = "0";

    /** 云 API：手机号，无则传 0 */
    private String mobile = "0";

    /** local=本地 API 插件；cloud=签名云 API */
    private String apiMode = "local";
}
