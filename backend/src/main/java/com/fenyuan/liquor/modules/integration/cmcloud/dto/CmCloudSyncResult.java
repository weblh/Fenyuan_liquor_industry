package com.fenyuan.liquor.modules.integration.cmcloud.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CmCloudSyncResult {
    private String sourceDb;
    private int customers;
    private int inventory;
    private int productStructure;
    private int priceCompare;
    private int customerDev;
    private String defaultWarehouse;
}
