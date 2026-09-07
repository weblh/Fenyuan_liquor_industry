package com.fenyuan.liquor.modules.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DashboardOverviewVO {

    /** 核心指标 */
    private Summary summary = new Summary();
    /** 在线销售趋势（按周期） */
    private List<OnlineSaleTrendItem> onlineSaleTrend = new ArrayList<>();
    /** 销售排名 */
    private List<SalesRankItem> salesRank = new ArrayList<>();
    /** 产品结构 */
    private List<ProductStructureItem> productStructure = new ArrayList<>();
    /** 库存概况 */
    private List<InventoryItem> inventory = new ArrayList<>();
    /** 客户开发 */
    private List<NameAmountItem> customerDev = new ArrayList<>();
    /** 应收账款 */
    private List<NameAmountItem> receivable = new ArrayList<>();
    /** 60天未复购客户（推送主看板） */
    private List<RetentionAlertItem> retentionAlerts = new ArrayList<>();
    /** 异地销售明细（地图） */
    private List<OffsiteSaleItem> offsiteSales = new ArrayList<>();

    @Data
    public static class Summary {
        private BigDecimal totalSaleAmount = BigDecimal.ZERO;
        private BigDecimal totalShipAmount = BigDecimal.ZERO;
        private BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        private BigDecimal totalInventoryAmount = BigDecimal.ZERO;
        private BigDecimal totalInventoryQty = BigDecimal.ZERO;
        private BigDecimal totalReceivableAmount = BigDecimal.ZERO;
        private BigDecimal totalCustomerDevAmount = BigDecimal.ZERO;
        private BigDecimal totalOffsiteQty = BigDecimal.ZERO;
        private long dealerCount;
        private long productCategoryCount;
        private long inventorySkuCount;
        private long customerDevCount;
        private long receivableCount;
        private long retentionAlertCount;
        private long offsiteProvinceCount;
    }

    @Data
    public static class OnlineSaleTrendItem {
        /** 美福 / 美团名酒行 */
        private String customerName;
        private String periodName;
        private BigDecimal saleAmount;
        private BigDecimal shipAmount;
        private BigDecimal paymentAmount;
    }

    @Data
    public static class SalesRankItem {
        private String companyName;
        private BigDecimal amount;
        private BigDecimal salesRatio;
        private Integer trend;
    }

    @Data
    public static class ProductStructureItem {
        private String series;
        private String productName;
        private String category;
        private BigDecimal quantity;
        private BigDecimal ratio;
        private String customerSource;
    }

    @Data
    public static class InventoryItem {
        private String productName;
        private String spec;
        private BigDecimal quantity;
        private BigDecimal amount;
        private String warehouse;
    }

    @Data
    public static class NameAmountItem {
        private String name;
        private BigDecimal amount;
        private String remark;
        private String openMonth;
    }

    @Data
    public static class RetentionAlertItem {
        private String customerName;
        private String contactPhone;
        private Integer daysSincePurchase;
        private String lastPurchaseDate;
        private String remark;
    }

    @Data
    public static class OffsiteSaleItem {
        private String productName;
        private BigDecimal quantity;
        private String province;
        private String city;
        private String address;
        private String sourceType;
    }
}
