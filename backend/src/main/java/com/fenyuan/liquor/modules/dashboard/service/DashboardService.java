package com.fenyuan.liquor.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenyuan.liquor.modules.business.customerdev.entity.BizCustomerDev;
import com.fenyuan.liquor.modules.business.customerdev.mapper.BizCustomerDevMapper;
import com.fenyuan.liquor.modules.business.inventory.entity.BizInventory;
import com.fenyuan.liquor.modules.business.inventory.mapper.BizInventoryMapper;
import com.fenyuan.liquor.modules.business.onlinesale.entity.BizOnlineSale;
import com.fenyuan.liquor.modules.business.onlinesale.mapper.BizOnlineSaleMapper;
import com.fenyuan.liquor.modules.business.productstructure.entity.BizProductStructure;
import com.fenyuan.liquor.modules.business.productstructure.mapper.BizProductStructureMapper;
import com.fenyuan.liquor.modules.business.salesrank.entity.BizSalesRank;
import com.fenyuan.liquor.modules.business.salesrank.mapper.BizSalesRankMapper;
import com.fenyuan.liquor.modules.dashboard.dto.DashboardOverviewVO;
import com.fenyuan.liquor.modules.dashboard.dto.DashboardOverviewVO.*;
import com.fenyuan.liquor.modules.finance.receivable.entity.FinReceivable;
import com.fenyuan.liquor.modules.finance.receivable.mapper.FinReceivableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BizOnlineSaleMapper onlineSaleMapper;
    private final BizSalesRankMapper salesRankMapper;
    private final BizInventoryMapper inventoryMapper;
    private final BizProductStructureMapper productStructureMapper;
    private final BizCustomerDevMapper customerDevMapper;
    private final FinReceivableMapper receivableMapper;

    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        Summary summary = vo.getSummary();

        List<BizOnlineSale> sales = onlineSaleMapper.selectList(
                new LambdaQueryWrapper<BizOnlineSale>().orderByAsc(BizOnlineSale::getId));
        for (BizOnlineSale item : sales) {
            summary.setTotalSaleAmount(add(summary.getTotalSaleAmount(), item.getSaleAmount()));
            summary.setTotalShipAmount(add(summary.getTotalShipAmount(), item.getShipAmount()));
            summary.setTotalPaymentAmount(add(summary.getTotalPaymentAmount(), item.getPaymentAmount()));
            OnlineSaleTrendItem trend = new OnlineSaleTrendItem();
            trend.setPeriodName(item.getPeriodName());
            trend.setSaleAmount(nvl(item.getSaleAmount()));
            trend.setShipAmount(nvl(item.getShipAmount()));
            trend.setPaymentAmount(nvl(item.getPaymentAmount()));
            vo.getOnlineSaleTrend().add(trend);
        }

        List<BizSalesRank> ranks = salesRankMapper.selectList(
                new LambdaQueryWrapper<BizSalesRank>().orderByDesc(BizSalesRank::getAmount));
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
                new LambdaQueryWrapper<BizInventory>().orderByDesc(BizInventory::getAmount));
        summary.setInventorySkuCount(inventories.size());
        for (BizInventory item : inventories) {
            summary.setTotalInventoryAmount(add(summary.getTotalInventoryAmount(), item.getAmount()));
            summary.setTotalInventoryQty(add(summary.getTotalInventoryQty(), item.getQuantity()));
            InventoryItem inv = new InventoryItem();
            inv.setProductName(item.getProductName());
            inv.setSpec(item.getSpec());
            inv.setQuantity(nvl(item.getQuantity()));
            inv.setAmount(nvl(item.getAmount()));
            inv.setWarehouse(item.getWarehouse());
            vo.getInventory().add(inv);
        }

        List<BizProductStructure> structures = productStructureMapper.selectList(
                new LambdaQueryWrapper<BizProductStructure>().orderByDesc(BizProductStructure::getRatio));
        summary.setProductCategoryCount(structures.size());
        for (BizProductStructure item : structures) {
            ProductStructureItem ps = new ProductStructureItem();
            ps.setCategory(item.getCategory());
            ps.setQuantity(nvl(item.getQuantity()));
            ps.setRatio(nvl(item.getRatio()));
            ps.setCustomerSource(item.getCustomerSource());
            vo.getProductStructure().add(ps);
        }

        List<BizCustomerDev> customers = customerDevMapper.selectList(
                new LambdaQueryWrapper<BizCustomerDev>().orderByDesc(BizCustomerDev::getAmount));
        summary.setCustomerDevCount(customers.size());
        for (BizCustomerDev item : customers) {
            summary.setTotalCustomerDevAmount(add(summary.getTotalCustomerDevAmount(), item.getAmount()));
            NameAmountItem na = new NameAmountItem();
            na.setName(item.getName());
            na.setAmount(nvl(item.getAmount()));
            na.setRemark(item.getRemark());
            vo.getCustomerDev().add(na);
        }

        List<FinReceivable> receivables = receivableMapper.selectList(
                new LambdaQueryWrapper<FinReceivable>().orderByDesc(FinReceivable::getAmount));
        summary.setReceivableCount(receivables.size());
        for (FinReceivable item : receivables) {
            summary.setTotalReceivableAmount(add(summary.getTotalReceivableAmount(), item.getAmount()));
            NameAmountItem na = new NameAmountItem();
            na.setName(item.getName());
            na.setAmount(nvl(item.getAmount()));
            na.setRemark(item.getRemark());
            vo.getReceivable().add(na);
        }

        // 趋势按周期名尽量保持时间顺序；无法解析时保持原序
        vo.setOnlineSaleTrend(vo.getOnlineSaleTrend().stream()
                .sorted(Comparator.comparing(OnlineSaleTrendItem::getPeriodName, Comparator.nullsLast(String::compareTo)))
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
