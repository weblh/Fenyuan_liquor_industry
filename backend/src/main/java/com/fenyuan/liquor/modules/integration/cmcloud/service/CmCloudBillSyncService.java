package com.fenyuan.liquor.modules.integration.cmcloud.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenyuan.liquor.common.config.CmCloudProperties;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.modules.business.common.BizErpRules;
import com.fenyuan.liquor.modules.business.customerdev.entity.BizCustomerDev;
import com.fenyuan.liquor.modules.business.customerdev.mapper.BizCustomerDevMapper;
import com.fenyuan.liquor.modules.business.customermaintain.entity.BizCustomerMaintain;
import com.fenyuan.liquor.modules.business.customermaintain.mapper.BizCustomerMaintainMapper;
import com.fenyuan.liquor.modules.business.inventory.entity.BizInventory;
import com.fenyuan.liquor.modules.business.inventory.mapper.BizInventoryMapper;
import com.fenyuan.liquor.modules.business.offsitesale.entity.BizOffsiteSale;
import com.fenyuan.liquor.modules.business.offsitesale.mapper.BizOffsiteSaleMapper;
import com.fenyuan.liquor.modules.business.onlinesale.entity.BizOnlineSale;
import com.fenyuan.liquor.modules.business.onlinesale.mapper.BizOnlineSaleMapper;
import com.fenyuan.liquor.modules.business.productstructure.entity.BizProductStructure;
import com.fenyuan.liquor.modules.business.productstructure.mapper.BizProductStructureMapper;
import com.fenyuan.liquor.modules.business.salesrank.entity.BizSalesRank;
import com.fenyuan.liquor.modules.business.salesrank.mapper.BizSalesRankMapper;
import com.fenyuan.liquor.modules.integration.cmcloud.client.CmCloudApiClient;
import com.fenyuan.liquor.modules.integration.cmcloud.dto.CmCloudBillSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmCloudBillSyncService {

    private static final String BILL_MARK = BizErpRules.ERP_BILL_SYNC_MARK;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CmCloudProperties properties;
    private final CmCloudApiClient apiClient;
    private final BizCustomerMaintainMapper customerMaintainMapper;
    private final BizCustomerDevMapper customerDevMapper;
    private final BizInventoryMapper inventoryMapper;
    private final BizSalesRankMapper salesRankMapper;
    private final BizOnlineSaleMapper onlineSaleMapper;
    private final BizProductStructureMapper productStructureMapper;
    private final BizOffsiteSaleMapper offsiteSaleMapper;

    @Transactional(rollbackFor = Exception.class)
    public CmCloudBillSyncResult syncBills() {
        if (!properties.isSyncEnabled()) {
            throw new BusinessException("管家婆同步已禁用");
        }

        LocalDate end = LocalDate.now();
        LocalDate begin = end.minusDays(Math.max(properties.getBillSyncDays(), 1));
        String beginDate = begin.toString();
        String endDate = end.toString();

        apiClient.ping();

        Map<String, String> customerAddress = loadCustomerAddresses();
        Map<String, LocalDate> customerLastPurchase = new HashMap<>();
        Map<String, LocalDate> customerFirstSale = new HashMap<>();
        Map<String, BigDecimal> dealerAmount = new HashMap<>();
        Map<String, BigDecimal> customerMonthAmount = new HashMap<>();
        Map<String, OnlineAgg> onlineAgg = new HashMap<>();
        Map<String, StructureAgg> structureAgg = new HashMap<>();
        Map<String, OffsiteAgg> offsiteAgg = new HashMap<>();

        int saleBillCount = 0;
        saleBillCount += processSaleBills(11, beginDate, endDate, 1, customerAddress,
                customerLastPurchase, customerFirstSale, dealerAmount, customerMonthAmount,
                onlineAgg, structureAgg, offsiteAgg);
        saleBillCount += processSaleBills(45, beginDate, endDate, -1, customerAddress,
                customerLastPurchase, customerFirstSale, dealerAmount, customerMonthAmount,
                onlineAgg, structureAgg, offsiteAgg);

        clearPreviousBillSync();
        int inventoryUpdated = syncInventoryQty();
        int salesRank = writeSalesRank(dealerAmount);
        int onlineSale = writeOnlineSale(onlineAgg);
        int productStructure = writeProductStructure(structureAgg);
        int offsiteSale = writeOffsiteSale(offsiteAgg);
        int customerUpdated = updateCustomerMaintain(customerLastPurchase);
        writeCustomerDev(customerFirstSale, customerMonthAmount);

        return CmCloudBillSyncResult.builder()
                .apiBaseUrl(properties.getApiBaseUrl())
                .beginDate(beginDate)
                .endDate(endDate)
                .saleBills(saleBillCount)
                .inventoryUpdated(inventoryUpdated)
                .salesRank(salesRank)
                .onlineSale(onlineSale)
                .productStructure(productStructure)
                .offsiteSale(offsiteSale)
                .customerMaintainUpdated(customerUpdated)
                .build();
    }

    /**
     * 不依赖财贸 API：从管家婆「销售明细表」Excel 导入全库房销售并汇总业务表。
     */
    @Transactional(rollbackFor = Exception.class)
    public CmCloudBillSyncResult importSalesExcel(InputStream in) {
        List<CmCloudReportExcelReader.SaleRow> sales = CmCloudReportExcelReader.readSales(in);
        if (sales.isEmpty()) {
            throw new BusinessException("销售明细 Excel 无有效数据行");
        }

        Map<String, String> customerAddress = loadCustomerAddresses();
        Map<String, LocalDate> customerLastPurchase = new HashMap<>();
        Map<String, LocalDate> customerFirstSale = new HashMap<>();
        Map<String, BigDecimal> dealerAmount = new HashMap<>();
        Map<String, BigDecimal> customerMonthAmount = new HashMap<>();
        Map<String, OnlineAgg> onlineAgg = new HashMap<>();
        Map<String, StructureAgg> structureAgg = new HashMap<>();
        Map<String, OffsiteAgg> offsiteAgg = new HashMap<>();

        LocalDate minDate = null;
        LocalDate maxDate = null;
        Set<String> billKeys = new LinkedHashSet<>();

        for (CmCloudReportExcelReader.SaleRow sale : sales) {
            billKeys.add(StringUtils.hasText(sale.billNo) ? sale.billNo : sale.customer + "|" + sale.billDate);
            if (sale.billDate != null) {
                customerLastPurchase.merge(sale.customer, sale.billDate, (a, b) -> a.isAfter(b) ? a : b);
                customerFirstSale.merge(sale.customer, sale.billDate, (a, b) -> a.isBefore(b) ? a : b);
                minDate = minDate == null || sale.billDate.isBefore(minDate) ? sale.billDate : minDate;
                maxDate = maxDate == null || sale.billDate.isAfter(maxDate) ? sale.billDate : maxDate;
            }
            if (sale.amount != null) {
                dealerAmount.merge(sale.customer, sale.amount, BigDecimal::add);
                if (sale.billDate != null) {
                    String monthKey = sale.customer + "|" + sale.billDate.format(MONTH_FMT);
                    customerMonthAmount.merge(monthKey, sale.amount, BigDecimal::add);
                }
            }

            String onlineKeyName = BizErpRules.resolveOnlineSaleCustomer(sale.customer);
            if (onlineKeyName != null && sale.billDate != null) {
                String month = sale.billDate.format(MONTH_FMT);
                String onlineKey = onlineKeyName + "|" + month;
                OnlineAgg agg = onlineAgg.computeIfAbsent(onlineKey,
                        k -> new OnlineAgg(onlineKeyName, month));
                if (sale.amount != null) {
                    agg.saleAmount = agg.saleAmount.add(sale.amount);
                    agg.paymentAmount = agg.paymentAmount.add(sale.amount);
                    agg.shipAmount = agg.saleAmount;
                }
            }

            String address = customerAddress.getOrDefault(sale.customer, "");
            String[] region = BizErpRules.resolveRegion(sale.customer, address);
            String regionText = StringUtils.hasText(address) ? address : sale.customer;
            BigDecimal qty = sale.quantity == null ? BigDecimal.ZERO : sale.quantity;

            String structureProduct = BizErpRules.resolveCanonicalProduct(
                    sale.productName, BizErpRules.structureProductCandidates());
            if (structureProduct != null) {
                StructureAgg sa = structureAgg.computeIfAbsent(structureProduct, StructureAgg::new);
                sa.quantity = sa.quantity.add(qty);
            }

            String offsiteProduct = BizErpRules.resolveCanonicalProduct(
                    sale.productName, BizErpRules.OFFSITE_PRODUCTS);
            if (offsiteProduct != null
                    && StringUtils.hasText(region[0])
                    && BizErpRules.isOutsideGuangzhou(region[0], region[1], regionText)) {
                String offsiteKey = offsiteProduct + "|" + region[0] + "|" + region[1];
                OffsiteAgg oa = offsiteAgg.computeIfAbsent(offsiteKey,
                        k -> new OffsiteAgg(offsiteProduct, region[0], region[1], regionText));
                oa.quantity = oa.quantity.add(qty);
            }
        }

        clearPreviousBillSync();
        int salesRank = writeSalesRank(dealerAmount);
        int onlineSale = writeOnlineSale(onlineAgg);
        int productStructure = writeProductStructure(structureAgg);
        int offsiteSale = writeOffsiteSale(offsiteAgg);
        int customerUpdated = updateCustomerMaintain(customerLastPurchase);
        writeCustomerDev(customerFirstSale, customerMonthAmount);

        return CmCloudBillSyncResult.builder()
                .apiBaseUrl("excel-import")
                .beginDate(minDate == null ? "" : minDate.toString())
                .endDate(maxDate == null ? "" : maxDate.toString())
                .saleBills(billKeys.size())
                .inventoryUpdated(0)
                .salesRank(salesRank)
                .onlineSale(onlineSale)
                .productStructure(productStructure)
                .offsiteSale(offsiteSale)
                .customerMaintainUpdated(customerUpdated)
                .build();
    }

    /**
     * 不依赖财贸 API：导入管家婆「存货库存详情」Excel。
     * 支持两种：①勾选「按存货汇总」（全仓按品名合计，无仓库列）；②分仓库明细（仅汇总后库+门店前厅）。
     */
    @Transactional(rollbackFor = Exception.class)
    public int importInventoryExcel(InputStream in) {
        List<CmCloudReportExcelReader.StockRow> stocks = CmCloudReportExcelReader.readStocks(in);
        if (stocks.isEmpty()) {
            throw new BusinessException("库存明细 Excel 无有效数据行");
        }

        boolean summaryMode = stocks.stream().anyMatch(s -> s.summaryByProduct);
        Map<String, StockAgg> byProduct = new LinkedHashMap<>();
        Set<String> hitWarehouses = new LinkedHashSet<>();

        for (CmCloudReportExcelReader.StockRow stock : stocks) {
            String targetWarehouse;
            if (stock.summaryByProduct
                    || BizErpRules.ALL_WAREHOUSE_SUMMARY_LABEL.equals(stock.warehouse)
                    || (StringUtils.hasText(stock.warehouse) && stock.warehouse.contains("全部仓库"))) {
                targetWarehouse = BizErpRules.ALL_WAREHOUSE_SUMMARY_LABEL;
                hitWarehouses.add("全部仓库");
            } else {
                String warehouseLabel = BizErpRules.resolveDashboardWarehouse(stock.warehouse);
                if (warehouseLabel == null) {
                    continue;
                }
                hitWarehouses.add(warehouseLabel);
                targetWarehouse = BizErpRules.COMBINED_WAREHOUSE_LABEL;
            }
            StockAgg agg = byProduct.computeIfAbsent(stock.productName, k -> new StockAgg());
            agg.productName = stock.productName;
            agg.warehouse = targetWarehouse;
            agg.quantity = agg.quantity.add(stock.quantity == null ? BigDecimal.ZERO : stock.quantity);
            if (stock.unitPrice != null && stock.unitPrice.compareTo(BigDecimal.ZERO) > 0) {
                agg.unitPrice = stock.unitPrice;
            }
            if (StringUtils.hasText(stock.spec)) {
                agg.spec = stock.spec;
            }
        }
        if (byProduct.isEmpty()) {
            throw new BusinessException(summaryMode
                    ? "按存货汇总 Excel 无有效数量行"
                    : "未匹配到「后库/门店前厅」库存行；若要全仓请勾选「按存货汇总」后导出");
        }

        inventoryMapper.delete(new LambdaQueryWrapper<BizInventory>()
                .and(w -> w.likeRight(BizInventory::getRemark, BizErpRules.ERP_SYNC_MARK)
                        .or()
                        .likeRight(BizInventory::getRemark, BILL_MARK)
                        .or()
                        .like(BizInventory::getWarehouse, "番禺")
                        .or()
                        .eq(BizInventory::getWarehouse, "主库")
                        .or()
                        .eq(BizInventory::getWarehouse, BizErpRules.ALL_WAREHOUSE_SUMMARY_LABEL)
                        .or()
                        .eq(BizInventory::getWarehouse, BizErpRules.COMBINED_WAREHOUSE_LABEL)));

        String remarkTag = summaryMode ? "按存货汇总" : String.join("+", hitWarehouses);
        int updated = 0;
        for (StockAgg stock : byProduct.values()) {
            if (stock.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal unitPrice = stock.unitPrice.compareTo(BigDecimal.ZERO) > 0
                    ? stock.unitPrice : BigDecimal.ZERO;
            BizInventory inv = new BizInventory();
            inv.setProductName(stock.productName);
            inv.setSpec(StringUtils.hasText(stock.spec) ? stock.spec : null);
            inv.setQuantity(stock.quantity);
            inv.setWarehouse(stock.warehouse);
            inv.setAmount(stock.quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            inv.setRemark(BILL_MARK + "STOCK:EXCEL:" + remarkTag + "] 存货库存详情导入 " + LocalDate.now());
            inventoryMapper.insert(inv);
            updated++;
        }
        return updated;
    }

    private int processSaleBills(int vchType, String beginDate, String endDate, int sign,
                                 Map<String, String> customerAddress,
                                 Map<String, LocalDate> customerLastPurchase,
                                 Map<String, LocalDate> customerFirstSale,
                                 Map<String, BigDecimal> dealerAmount,
                                 Map<String, BigDecimal> customerMonthAmount,
                                 Map<String, OnlineAgg> onlineAgg,
                                 Map<String, StructureAgg> structureAgg,
                                 Map<String, OffsiteAgg> offsiteAgg) {
        JSONArray bills = apiClient.fetchAll("DlySaleData", param -> {
            param.put("VchType", vchType);
            param.put("BeginDate", beginDate);
            param.put("EndDate", endDate);
        });

        for (int i = 0; i < bills.size(); i++) {
            JSONObject bill = bills.getJSONObject(i);
            String customer = text(bill, "bfullname");
            if (!StringUtils.hasText(customer)) {
                continue;
            }
            LocalDate billDate = parseDate(text(bill, "date"));
            if (billDate != null) {
                customerLastPurchase.merge(customer, billDate, (a, b) -> a.isAfter(b) ? a : b);
                // 退货单不参与「首单」判定
                if (sign > 0) {
                    customerFirstSale.merge(customer, billDate, (a, b) -> a.isBefore(b) ? a : b);
                }
            }

            BigDecimal billTotal = decimal(bill, "billtotal");
            if (billTotal == null) {
                billTotal = decimal(bill, "total");
            }
            if (billTotal != null) {
                BigDecimal signed = billTotal.multiply(BigDecimal.valueOf(sign));
                dealerAmount.merge(customer, signed, BigDecimal::add);
                if (billDate != null) {
                    String monthKey = customer + "|" + billDate.format(MONTH_FMT);
                    customerMonthAmount.merge(monthKey, signed, BigDecimal::add);
                }
            }

            String onlineKeyName = BizErpRules.resolveOnlineSaleCustomer(customer);
            if (onlineKeyName != null && billDate != null) {
                String month = billDate.format(MONTH_FMT);
                String onlineKey = onlineKeyName + "|" + month;
                OnlineAgg agg = onlineAgg.computeIfAbsent(onlineKey, k -> new OnlineAgg(onlineKeyName, month));
                if (billTotal != null) {
                    agg.saleAmount = agg.saleAmount.add(billTotal.multiply(BigDecimal.valueOf(sign)));
                }
                BigDecimal paid = sumSettle(bill);
                if (paid != null) {
                    agg.paymentAmount = agg.paymentAmount.add(paid.multiply(BigDecimal.valueOf(sign)));
                } else if (billTotal != null) {
                    agg.paymentAmount = agg.paymentAmount.add(billTotal.multiply(BigDecimal.valueOf(sign)));
                }
                agg.shipAmount = agg.saleAmount;
            }

            JSONArray details = bill.getJSONArray("detail");
            if (details == null) {
                continue;
            }
            String address = customerAddress.getOrDefault(customer, "");
            String[] region = BizErpRules.resolveRegion(customer, address);
            String regionText = StringUtils.hasText(address) ? address : customer;

            for (int d = 0; d < details.size(); d++) {
                JSONObject line = details.getJSONObject(d);
                String productName = text(line, "pfullname");
                BigDecimal qty = decimal(line, "qty");
                if (qty == null) {
                    continue;
                }
                qty = qty.multiply(BigDecimal.valueOf(sign));

                String structureProduct = BizErpRules.resolveCanonicalProduct(
                        productName, BizErpRules.structureProductCandidates());
                if (structureProduct != null) {
                    StructureAgg sa = structureAgg.computeIfAbsent(structureProduct, StructureAgg::new);
                    sa.quantity = sa.quantity.add(qty);
                }

                String offsiteProduct = BizErpRules.resolveCanonicalProduct(
                        productName, BizErpRules.OFFSITE_PRODUCTS);
                if (offsiteProduct != null
                        && StringUtils.hasText(region[0])
                        && BizErpRules.isOutsideGuangzhou(region[0], region[1], regionText)) {
                    String offsiteKey = offsiteProduct + "|" + region[0] + "|" + region[1];
                    OffsiteAgg oa = offsiteAgg.computeIfAbsent(offsiteKey,
                            k -> new OffsiteAgg(offsiteProduct, region[0], region[1], regionText));
                    oa.quantity = oa.quantity.add(qty);
                }
            }
        }
        return bills.size();
    }

    private int syncInventoryQty() {
        JSONArray stocks = apiClient.fetchAll("GoodsStocks", param -> {
        });

        // 仅 02后库 / 05门店前厅，按品名汇总（两仓集合到一起）
        Map<String, StockAgg> byProduct = new LinkedHashMap<>();
        Set<String> hitWarehouses = new LinkedHashSet<>();
        for (int i = 0; i < stocks.size(); i++) {
            JSONObject row = stocks.getJSONObject(i);
            String name = firstNonBlank(text(row, "PFullName"), text(row, "pfullname"), text(row, "PName"));
            if (!StringUtils.hasText(name)) {
                continue;
            }
            BigDecimal qty = firstNonNull(decimal(row, "Qty"), decimal(row, "qty"), decimal(row, "Qty1"));
            if (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String warehouseRaw = firstNonBlank(
                    text(row, "KFullName"), text(row, "kfullname"), text(row, "KName"),
                    text(row, "KUserCode"), text(row, "kUserCode"), text(row, "StockCode"));
            String warehouseLabel = BizErpRules.resolveDashboardWarehouse(warehouseRaw);
            if (warehouseLabel == null) {
                continue;
            }
            hitWarehouses.add(warehouseLabel);
            StockAgg agg = byProduct.computeIfAbsent(name, k -> new StockAgg());
            agg.productName = name;
            agg.warehouse = BizErpRules.COMBINED_WAREHOUSE_LABEL;
            agg.quantity = agg.quantity.add(qty);
            BigDecimal price = firstNonNull(decimal(row, "Price"), decimal(row, "price"),
                    decimal(row, "RetailPrice"), decimal(row, "CostPrice"));
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                agg.unitPrice = price;
            }
            String spec = firstNonBlank(text(row, "Standard"), text(row, "standard"), text(row, "PStandard"));
            if (StringUtils.hasText(spec)) {
                agg.spec = spec;
            }
        }

        // 清掉历史同步行（含误写入的番禺库主档库存）
        inventoryMapper.delete(new LambdaQueryWrapper<BizInventory>()
                .and(w -> w.likeRight(BizInventory::getRemark, BizErpRules.ERP_SYNC_MARK)
                        .or()
                        .likeRight(BizInventory::getRemark, BILL_MARK)
                        .or()
                        .like(BizInventory::getWarehouse, "番禺")
                        .or()
                        .eq(BizInventory::getWarehouse, "主库")));

        int updated = 0;
        for (StockAgg stock : byProduct.values()) {
            if (stock.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal unitPrice = stock.unitPrice.compareTo(BigDecimal.ZERO) > 0
                    ? stock.unitPrice : BigDecimal.ZERO;
            BizInventory inv = new BizInventory();
            inv.setProductName(stock.productName);
            inv.setSpec(StringUtils.hasText(stock.spec) ? stock.spec : null);
            inv.setQuantity(stock.quantity);
            inv.setWarehouse(BizErpRules.COMBINED_WAREHOUSE_LABEL);
            inv.setAmount(stock.quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            inv.setRemark(BILL_MARK + "STOCK:" + String.join("+", hitWarehouses) + "] 存货库存详情汇总 "
                    + LocalDate.now());
            inventoryMapper.insert(inv);
            updated++;
        }
        log.info("库存同步完成：API行={}，命中仓={}，汇总SKU={}", stocks.size(), hitWarehouses, updated);
        return updated;
    }

    private int writeSalesRank(Map<String, BigDecimal> dealerAmount) {
        BigDecimal total = dealerAmount.values().stream()
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(dealerAmount.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry<String, BigDecimal>::getValue).reversed());

        int n = 0;
        for (Map.Entry<String, BigDecimal> e : sorted) {
            if (e.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BizSalesRank row = new BizSalesRank();
            row.setCompanyName(e.getKey());
            row.setAmount(e.getValue().setScale(2, RoundingMode.HALF_UP));
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                row.setSalesRatio(e.getValue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(total, 2, RoundingMode.HALF_UP));
            } else {
                row.setSalesRatio(BigDecimal.ZERO);
            }
            row.setTrend(0);
            row.setRemark(BILL_MARK + "RANK] 销售排名同步");
            salesRankMapper.insert(row);
            n++;
        }
        return n;
    }

    private int writeOnlineSale(Map<String, OnlineAgg> onlineAgg) {
        int n = 0;
        for (OnlineAgg agg : onlineAgg.values()) {
            if (agg.saleAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BizOnlineSale row = new BizOnlineSale();
            row.setCustomerName(agg.customerName);
            row.setPeriodName(agg.periodName);
            row.setSaleAmount(agg.saleAmount.setScale(2, RoundingMode.HALF_UP));
            row.setShipAmount(agg.shipAmount.setScale(2, RoundingMode.HALF_UP));
            row.setPaymentAmount(agg.paymentAmount.setScale(2, RoundingMode.HALF_UP));
            row.setRemark(BILL_MARK + "ONLINE:" + agg.customerName + ":" + agg.periodName + "]");
            onlineSaleMapper.insert(row);
            n++;
        }
        return n;
    }

    private int writeProductStructure(Map<String, StructureAgg> structureAgg) {
        BigDecimal totalQty = structureAgg.values().stream()
                .map(a -> a.quantity)
                .filter(q -> q.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int n = 0;
        for (Map.Entry<String, StructureAgg> e : structureAgg.entrySet()) {
            StructureAgg agg = e.getValue();
            if (agg.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BizProductStructure row = new BizProductStructure();
            row.setSeries(BizErpRules.seriesOf(e.getKey()));
            row.setProductName(e.getKey());
            row.setCategory(e.getKey());
            row.setQuantity(agg.quantity.setScale(2, RoundingMode.HALF_UP));
            if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                row.setRatio(agg.quantity.multiply(BigDecimal.valueOf(100))
                        .divide(totalQty, 2, RoundingMode.HALF_UP));
            } else {
                row.setRatio(BigDecimal.ZERO);
            }
            row.setCustomerSource("ERP销售单");
            row.setRemark(BILL_MARK + "STRUCT:" + e.getKey() + "]");
            productStructureMapper.insert(row);
            n++;
        }
        return n;
    }

    private int writeOffsiteSale(Map<String, OffsiteAgg> offsiteAgg) {
        int n = 0;
        for (OffsiteAgg agg : offsiteAgg.values()) {
            if (agg.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BizOffsiteSale row = new BizOffsiteSale();
            row.setProductName(agg.productName);
            row.setQuantity(agg.quantity.setScale(2, RoundingMode.HALF_UP));
            row.setProvince(agg.province);
            row.setCity(agg.city);
            row.setAddress(agg.address);
            row.setSourceType("ERP");
            row.setRemark(BILL_MARK + "OFFSITE:" + agg.productName + "]");
            offsiteSaleMapper.insert(row);
            n++;
        }
        return n;
    }

    private int updateCustomerMaintain(Map<String, LocalDate> customerLastPurchase) {
        List<BizCustomerMaintain> rows = customerMaintainMapper.selectList(
                new LambdaQueryWrapper<BizCustomerMaintain>()
                        .likeRight(BizCustomerMaintain::getRemark, BizErpRules.ERP_SYNC_MARK));
        LocalDate today = LocalDate.now();
        int updated = 0;
        for (BizCustomerMaintain row : rows) {
            LocalDate last = customerLastPurchase.get(row.getCustomerName());
            if (last == null && StringUtils.hasText(row.getLastPurchaseDate())) {
                last = parseDate(row.getLastPurchaseDate());
            }
            if (last == null) {
                // 无购买记录：显式写 null，避免 DEFAULT 0 造成「未复购天数都是0」
                row.setLastPurchaseDate(null);
                row.setDaysSincePurchase(null);
                row.setAlertStatus(0);
            } else {
                int days = (int) ChronoUnit.DAYS.between(last, today);
                if (days < 0) {
                    days = 0;
                }
                row.setLastPurchaseDate(last.toString());
                row.setDaysSincePurchase(days);
                row.setAlertStatus(days >= 60 ? 1 : 0);
            }
            customerMaintainMapper.updateById(row);
            updated++;
        }
        return updated;
    }

    /**
     * 客户开发（销售客户）：历史窗口内此前从未销售过，且当月首次产生销售。
     * 金额取当月销售合计。
     */
    private void writeCustomerDev(Map<String, LocalDate> customerFirstSale,
                                  Map<String, BigDecimal> customerMonthAmount) {
        if (customerFirstSale.isEmpty()) {
            return;
        }
        String currentMonth = LocalDate.now().format(MONTH_FMT);
        List<Map.Entry<String, LocalDate>> sorted = new ArrayList<>(customerFirstSale.entrySet());
        sorted.sort(Comparator
                .comparing((Map.Entry<String, LocalDate> e) -> e.getValue(), Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));

        for (Map.Entry<String, LocalDate> e : sorted) {
            String name = e.getKey();
            if (!StringUtils.hasText(name)) {
                continue;
            }
            // 在线渠道客户本身不计入「新开发」
            if (BizErpRules.isOnlineSaleCustomer(name)) {
                continue;
            }
            LocalDate first = e.getValue();
            if (first == null) {
                continue;
            }
            // 仅当月首单：首单月=当月 ⇒ 历史无更早销售
            if (!currentMonth.equals(first.format(MONTH_FMT))) {
                continue;
            }
            BigDecimal amount = customerMonthAmount.getOrDefault(name + "|" + currentMonth, BigDecimal.ZERO);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BizCustomerDev row = new BizCustomerDev();
            row.setName(name);
            row.setOpenMonth(currentMonth);
            row.setStatus(1);
            row.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            row.setRemark(BILL_MARK + "DEV:" + name + "] 当月首单=" + first + "（此前未销售）");
            customerDevMapper.insert(row);
        }
    }

    private Map<String, String> loadCustomerAddresses() {
        Map<String, String> map = new HashMap<>();
        List<BizCustomerMaintain> rows = customerMaintainMapper.selectList(
                new LambdaQueryWrapper<BizCustomerMaintain>()
                        .likeRight(BizCustomerMaintain::getRemark, BizErpRules.ERP_SYNC_MARK));
        for (BizCustomerMaintain row : rows) {
            String addr = BizErpRules.parseAddressFromRemark(row.getRemark());
            if (StringUtils.hasText(addr)) {
                map.put(row.getCustomerName(), addr);
            }
        }
        return map;
    }

    private void clearPreviousBillSync() {
        offsiteSaleMapper.delete(new LambdaQueryWrapper<BizOffsiteSale>()
                .likeRight(BizOffsiteSale::getRemark, BILL_MARK));
        onlineSaleMapper.delete(new LambdaQueryWrapper<BizOnlineSale>()
                .likeRight(BizOnlineSale::getRemark, BILL_MARK));
        salesRankMapper.delete(new LambdaQueryWrapper<BizSalesRank>()
                .likeRight(BizSalesRank::getRemark, BILL_MARK));
        productStructureMapper.delete(new LambdaQueryWrapper<BizProductStructure>()
                .likeRight(BizProductStructure::getRemark, BILL_MARK));
        customerDevMapper.delete(new LambdaQueryWrapper<BizCustomerDev>()
                .likeRight(BizCustomerDev::getRemark, BILL_MARK));
    }

    private BigDecimal sumSettle(JSONObject bill) {
        JSONArray settle = bill.getJSONArray("settle");
        if (settle == null || settle.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < settle.size(); i++) {
            BigDecimal t = decimal(settle.getJSONObject(i), "total");
            if (t != null) {
                sum = sum.add(t.abs());
            }
        }
        return sum;
    }

    private LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (Exception e) {
            return null;
        }
    }

    private String text(JSONObject obj, String key) {
        if (obj == null || !obj.containsKey(key)) {
            return "";
        }
        Object v = obj.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private BigDecimal decimal(JSONObject obj, String key) {
        if (obj == null || !obj.containsKey(key)) {
            return null;
        }
        Object v = obj.get(key);
        if (v == null || !StringUtils.hasText(String.valueOf(v))) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return "";
    }

    private BigDecimal firstNonNull(BigDecimal... values) {
        for (BigDecimal v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static class OnlineAgg {
        final String customerName;
        final String periodName;
        BigDecimal saleAmount = BigDecimal.ZERO;
        BigDecimal shipAmount = BigDecimal.ZERO;
        BigDecimal paymentAmount = BigDecimal.ZERO;

        OnlineAgg(String customerName, String periodName) {
            this.customerName = customerName;
            this.periodName = periodName;
        }
    }

    private static class StructureAgg {
        BigDecimal quantity = BigDecimal.ZERO;

        StructureAgg(String product) {
        }
    }

    private static class OffsiteAgg {
        final String productName;
        final String province;
        final String city;
        final String address;
        BigDecimal quantity = BigDecimal.ZERO;

        OffsiteAgg(String productName, String province, String city, String address) {
            this.productName = productName;
            this.province = province;
            this.city = city;
            this.address = address;
        }
    }

    private static class StockAgg {
        String productName = "";
        String warehouse = "";
        String spec = "";
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
    }
}
