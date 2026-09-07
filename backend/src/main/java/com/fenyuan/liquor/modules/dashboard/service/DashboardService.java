package com.fenyuan.liquor.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.fenyuan.liquor.modules.dashboard.dto.DashboardOverviewVO;
import com.fenyuan.liquor.modules.dashboard.dto.DashboardOverviewVO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BizOnlineSaleMapper onlineSaleMapper;
    private final BizSalesRankMapper salesRankMapper;
    private final BizInventoryMapper inventoryMapper;
    private final BizProductStructureMapper productStructureMapper;
    private final BizCustomerDevMapper customerDevMapper;
    private final BizCustomerMaintainMapper customerMaintainMapper;
    private final BizOffsiteSaleMapper offsiteSaleMapper;

    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        Summary summary = vo.getSummary();

        List<BizOnlineSale> sales = onlineSaleMapper.selectList(
                new LambdaQueryWrapper<BizOnlineSale>().orderByAsc(BizOnlineSale::getId));
        for (BizOnlineSale item : sales) {
            if (!BizErpRules.isOnlineSaleCustomer(item.getCustomerName())) {
                continue;
            }
            summary.setTotalSaleAmount(add(summary.getTotalSaleAmount(), item.getSaleAmount()));
            summary.setTotalShipAmount(add(summary.getTotalShipAmount(), item.getShipAmount()));
            summary.setTotalPaymentAmount(add(summary.getTotalPaymentAmount(), item.getPaymentAmount()));
            OnlineSaleTrendItem trend = new OnlineSaleTrendItem();
            String channel = BizErpRules.resolveOnlineSaleCustomer(item.getCustomerName());
            trend.setCustomerName(channel != null ? channel : item.getCustomerName());
            trend.setPeriodName(item.getPeriodName());
            trend.setSaleAmount(nvl(item.getSaleAmount()));
            trend.setShipAmount(nvl(item.getShipAmount()));
            trend.setPaymentAmount(nvl(item.getPaymentAmount()));
            vo.getOnlineSaleTrend().add(trend);
        }

        List<BizSalesRank> ranks = salesRankMapper.selectList(
                new LambdaQueryWrapper<BizSalesRank>()
                        .gt(BizSalesRank::getAmount, BigDecimal.ZERO)
                        .orderByDesc(BizSalesRank::getAmount));
        summary.setDealerCount(ranks.size());
        for (BizSalesRank item : ranks) {
            SalesRankItem rankItem = new SalesRankItem();
            rankItem.setCompanyName(item.getCompanyName());
            rankItem.setAmount(nvl(item.getAmount()));
            rankItem.setSalesRatio(nvl(item.getSalesRatio()));
            rankItem.setTrend(item.getTrend() == null ? 0 : item.getTrend());
            vo.getSalesRank().add(rankItem);
        }

        List<BizInventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<BizInventory>()
                        .and(w -> w.likeRight(BizInventory::getRemark, BizErpRules.ERP_SYNC_MARK)
                                .or()
                                .likeRight(BizInventory::getRemark, BizErpRules.ERP_BILL_SYNC_MARK))
                        .gt(BizInventory::getQuantity, BigDecimal.ZERO)
                        .orderByDesc(BizInventory::getQuantity));
        Map<String, InventoryItem> invMerged = new LinkedHashMap<>();
        for (BizInventory item : inventories) {
            if (StringUtils.hasText(item.getWarehouse())
                    && !BizErpRules.isDashboardWarehouse(item.getWarehouse())
                    && !BizErpRules.COMBINED_WAREHOUSE_LABEL.equals(item.getWarehouse())
                    && !BizErpRules.ALL_WAREHOUSE_SUMMARY_LABEL.equals(item.getWarehouse())
                    && !item.getWarehouse().contains("前厅")
                    && !item.getWarehouse().contains("后库")
                    && !item.getWarehouse().contains("全部仓库")) {
                continue;
            }
            summary.setTotalInventoryAmount(add(summary.getTotalInventoryAmount(), item.getAmount()));
            summary.setTotalInventoryQty(add(summary.getTotalInventoryQty(), item.getQuantity()));
            InventoryItem inv = invMerged.computeIfAbsent(item.getProductName(), k -> {
                InventoryItem created = new InventoryItem();
                created.setProductName(item.getProductName());
                created.setSpec(item.getSpec());
                created.setQuantity(BigDecimal.ZERO);
                created.setAmount(BigDecimal.ZERO);
                created.setWarehouse(BizErpRules.COMBINED_WAREHOUSE_LABEL);
                return created;
            });
            inv.setQuantity(add(inv.getQuantity(), item.getQuantity()));
            inv.setAmount(add(inv.getAmount(), item.getAmount()));
            inv.setWarehouse(BizErpRules.COMBINED_WAREHOUSE_LABEL);
        }
        List<InventoryItem> invList = new ArrayList<>(invMerged.values());
        invList.sort(Comparator.comparing(InventoryItem::getQuantity, Comparator.nullsLast(Comparator.reverseOrder())));
        summary.setInventorySkuCount(invList.size());
        // 看板仅展示数量靠前的部分产品（从多到少）
        int limit = Math.min(15, invList.size());
        vo.getInventory().addAll(invList.subList(0, limit));

        List<BizProductStructure> structures = productStructureMapper.selectList(
                new LambdaQueryWrapper<BizProductStructure>()
                        .orderByAsc(BizProductStructure::getSeries)
                        .orderByDesc(BizProductStructure::getQuantity));
        summary.setProductCategoryCount(structures.size());
        for (BizProductStructure item : structures) {
            ProductStructureItem ps = new ProductStructureItem();
            ps.setSeries(StringUtils.hasText(item.getSeries()) ? item.getSeries() : item.getCategory());
            ps.setProductName(item.getProductName());
            ps.setCategory(item.getCategory());
            ps.setQuantity(nvl(item.getQuantity()));
            ps.setRatio(nvl(item.getRatio()));
            ps.setCustomerSource(item.getCustomerSource());
            vo.getProductStructure().add(ps);
        }

        List<BizCustomerDev> customers = customerDevMapper.selectList(
                new LambdaQueryWrapper<BizCustomerDev>()
                        .eq(BizCustomerDev::getStatus, 1)
                        .orderByDesc(BizCustomerDev::getOpenMonth)
                        .orderByDesc(BizCustomerDev::getAmount));
        summary.setCustomerDevCount(customers.size());
        for (BizCustomerDev item : customers) {
            summary.setTotalCustomerDevAmount(add(summary.getTotalCustomerDevAmount(), item.getAmount()));
            NameAmountItem na = new NameAmountItem();
            na.setName(item.getName());
            na.setAmount(nvl(item.getAmount()));
            na.setRemark(item.getRemark());
            na.setOpenMonth(item.getOpenMonth());
            vo.getCustomerDev().add(na);
        }

        List<BizCustomerMaintain> alerts = customerMaintainMapper.selectList(
                new LambdaQueryWrapper<BizCustomerMaintain>()
                        .eq(BizCustomerMaintain::getAlertStatus, 1)
                        .likeRight(BizCustomerMaintain::getRemark, BizErpRules.ERP_SYNC_MARK)
                        .isNotNull(BizCustomerMaintain::getDaysSincePurchase)
                        .ge(BizCustomerMaintain::getDaysSincePurchase, 60)
                        .orderByDesc(BizCustomerMaintain::getDaysSincePurchase)
                        .last("LIMIT 20"));
        summary.setRetentionAlertCount(alerts.size());
        for (BizCustomerMaintain item : alerts) {
            RetentionAlertItem alert = new RetentionAlertItem();
            alert.setCustomerName(item.getCustomerName());
            alert.setContactPhone(item.getContactPhone());
            alert.setDaysSincePurchase(item.getDaysSincePurchase());
            alert.setLastPurchaseDate(item.getLastPurchaseDate());
            alert.setRemark(item.getRemark());
            vo.getRetentionAlerts().add(alert);
        }

        List<BizOffsiteSale> offsites = offsiteSaleMapper.selectList(
                new LambdaQueryWrapper<BizOffsiteSale>().orderByDesc(BizOffsiteSale::getQuantity));
        Set<String> provinces = new HashSet<>();
        for (BizOffsiteSale item : offsites) {
            if (!BizErpRules.matchesAnyOffsiteProduct(item.getProductName())) {
                continue;
            }
            if (!BizErpRules.isOutsideGuangzhou(item.getProvince(), item.getCity(), item.getAddress())) {
                continue;
            }
            summary.setTotalOffsiteQty(add(summary.getTotalOffsiteQty(), item.getQuantity()));
            if (item.getProvince() != null && !item.getProvince().isEmpty()) {
                provinces.add(item.getProvince());
            }
            OffsiteSaleItem os = new OffsiteSaleItem();
            os.setProductName(item.getProductName());
            os.setQuantity(nvl(item.getQuantity()));
            os.setProvince(item.getProvince());
            os.setCity(item.getCity());
            os.setAddress(item.getAddress());
            os.setSourceType(item.getSourceType());
            vo.getOffsiteSales().add(os);
        }
        summary.setOffsiteProvinceCount(provinces.size());

        vo.setOnlineSaleTrend(vo.getOnlineSaleTrend().stream()
                .sorted(Comparator
                        .comparing(OnlineSaleTrendItem::getPeriodName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(OnlineSaleTrendItem::getCustomerName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList()));

        return vo;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nvl(a).add(nvl(b));
    }
}
