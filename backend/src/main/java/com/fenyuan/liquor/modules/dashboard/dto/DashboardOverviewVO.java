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

    @Data
    public static class Summary {
        private BigDecimal totalSaleAmount = BigDecimal.ZERO;
        private BigDecimal totalShipAmount = BigDecimal.ZERO;
        private BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        private BigDecimal totalInventoryAmount = BigDecimal.ZERO;
        private BigDecimal totalInventoryQty = BigDecimal.ZERO;
        private BigDecimal totalReceivableAmount = BigDecimal.ZERO;
        private BigDecimal totalCustomerDevAmount = BigDecimal.ZERO;
        private long dealerCount;
        private long productCategoryCount;
        private long inventorySkuCount;
        private long customerDevCount;
        private long receivableCount;
    }

    @Data
    public static class OnlineSaleTrendItem {
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
    }
}
