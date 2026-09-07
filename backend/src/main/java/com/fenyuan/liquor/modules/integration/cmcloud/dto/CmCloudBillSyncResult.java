package com.fenyuan.liquor.modules.integration.cmcloud.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CmCloudBillSyncResult {
    private String apiBaseUrl;
    private String beginDate;
    private String endDate;
    private int saleBills;
    private int inventoryUpdated;
    private int salesRank;
    private int onlineSale;
    private int productStructure;
    private int offsiteSale;
    private int customerMaintainUpdated;
}
