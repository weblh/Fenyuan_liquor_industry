package com.fenyuan.liquor.modules.business.pricecompare.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.common.BizErpRules;
import com.fenyuan.liquor.modules.business.pricecompare.entity.BizPriceCompare;
import com.fenyuan.liquor.modules.business.pricecompare.mapper.BizPriceCompareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BizPriceCompareService {

    private static final DateTimeFormatter CRAWL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 线上价超过该小时数自动重抓 */
    private static final long STALE_HOURS = 12;

    private final BizPriceCompareMapper mapper;
    private final OnlinePriceCrawlService crawlService;

    public PageResult<BizPriceCompare> page(long current, long size, String productName) {
        ensureBaselineProducts();
        Page<BizPriceCompare> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizPriceCompare>()
                        .like(StrUtil.isNotBlank(productName), BizPriceCompare::getProductName, productName)
                        .and(w -> {
                            for (int i = 0; i < BizErpRules.PRICE_COMPARE_PRODUCTS.size(); i++) {
                                String name = BizErpRules.PRICE_COMPARE_PRODUCTS.get(i);
                                if (i == 0) {
                                    w.eq(BizPriceCompare::getProductName, name);
                                } else {
                                    w.or().eq(BizPriceCompare::getProductName, name);
                                }
                            }
                        })
                        .orderByAsc(BizPriceCompare::getId));
        return PageResult.of(page);
    }

    /**
     * 强制拉取京东/天猫官方店实时价。
     */
    public Map<String, String> crawlOnlinePrices() {
        ensureBaselineProducts();
        return refreshOnlinePrices(true);
    }

    private Map<String, String> refreshOnlinePrices(boolean force) {
        Map<String, String> report = new HashMap<>();
        List<BizPriceCompare> rows = mapper.selectList(new LambdaQueryWrapper<BizPriceCompare>()
                .in(BizPriceCompare::getProductName, BizErpRules.PRICE_COMPARE_PRODUCTS));
        String now = LocalDateTime.now().format(CRAWL_FMT);
        for (BizPriceCompare row : rows) {
            String name = row.getProductName();
            if (!force && !isStale(row) && hasAnyOnlinePrice(row)) {
                report.put(name, "未过期，跳过");
                continue;
            }
            try {
                OnlinePriceCrawlService.PlatformPrice price = crawlService.crawl(name);
                boolean updated = false;
                if (price.getJdPrice() != null) {
                    row.setJdPrice(price.getJdPrice());
                    updated = true;
                }
                if (price.getTmallPrice() != null) {
                    row.setTmallPrice(price.getTmallPrice());
                    updated = true;
                }
                if (updated) {
                    row.setCrawlTime(now);
                    String unit = BizErpRules.isNetworkPriceOnlyProduct(name) ? "元/瓶" : "元/件";
                    row.setRemark(buildRemark(name, price, unit));
                    mapper.updateById(row);
                    report.put(name, "京东=" + nvl(price.getJdPrice()) + " 天猫=" + nvl(price.getTmallPrice()));
                } else {
                    report.put(name, "线上价暂不可用（站点防爬），请稍后重试");
                    log.warn("价格抓取无结果: {}", name);
                }
            } catch (Exception e) {
                report.put(name, "抓取失败: " + e.getMessage());
                log.warn("价格抓取异常 {}: {}", name, e.getMessage());
            }
        }
        return report;
    }

    private boolean isStale(BizPriceCompare row) {
        if (!StringUtils.hasText(row.getCrawlTime())) {
            return true;
        }
        try {
            LocalDateTime t = LocalDateTime.parse(row.getCrawlTime(), CRAWL_FMT);
            return ChronoUnit.HOURS.between(t, LocalDateTime.now()) >= STALE_HOURS;
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean hasAnyOnlinePrice(BizPriceCompare row) {
        return (row.getJdPrice() != null && row.getJdPrice().compareTo(BigDecimal.ZERO) > 0)
                || (row.getTmallPrice() != null && row.getTmallPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    private static String nvl(BigDecimal v) {
        return v == null ? "-" : v.toPlainString();
    }

    private String buildRemark(String canonical, OnlinePriceCrawlService.PlatformPrice price, String unit) {
        StringBuilder sb = new StringBuilder();
        if (BizErpRules.isNetworkPriceOnlyProduct(canonical)) {
            sb.append("仅摘取网络实时价");
        } else {
            BigDecimal fenyuan = BizErpRules.fenyuanSalePriceOf(canonical);
            if (fenyuan != null) {
                sb.append("汾源销售价 ").append(fenyuan.toPlainString()).append(" 元/件");
            }
        }
        if (price.getJdPrice() != null) {
            sb.append("；京东官方店 ").append(price.getJdPrice().toPlainString()).append(unit);
        }
        if (price.getTmallPrice() != null) {
            sb.append("；天猫官方店 ").append(price.getTmallPrice().toPlainString()).append(unit);
        }
        return sb.toString();
    }

    /**
     * 保证比价清单始终有客户确认的产品行，并写入汾源销售价（飞天茅台除外）。
     */
    public void ensureBaselineProducts() {
        List<BizPriceCompare> existing = mapper.selectList(new LambdaQueryWrapper<>());
        Set<String> names = existing.stream()
                .map(BizPriceCompare::getProductName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        for (String canonical : BizErpRules.PRICE_COMPARE_PRODUCTS) {
            BizPriceCompare row = existing.stream()
                    .filter(e -> canonical.equals(e.getProductName())
                            || BizErpRules.matchesCanonicalProduct(e.getProductName(), canonical))
                    .findFirst()
                    .orElse(null);
            if (row == null) {
                if (names.contains(canonical)) {
                    continue;
                }
                row = new BizPriceCompare();
                row.setProductName(canonical);
                row.setSpec(BizErpRules.specOf(canonical));
                applyFenyuanPrice(row, canonical);
                row.setRemark(defaultRemark(canonical));
                mapper.insert(row);
            } else {
                boolean dirty = false;
                if (!canonical.equals(row.getProductName())) {
                    row.setProductName(canonical);
                    dirty = true;
                }
                if (!StringUtils.hasText(row.getSpec())) {
                    row.setSpec(BizErpRules.specOf(canonical));
                    dirty = true;
                }
                if (applyFenyuanPrice(row, canonical)) {
                    dirty = true;
                }
                if (dirty) {
                    if (!StringUtils.hasText(row.getRemark())) {
                        row.setRemark(defaultRemark(canonical));
                    }
                    mapper.updateById(row);
                }
            }
        }
    }

    private boolean applyFenyuanPrice(BizPriceCompare row, String canonical) {
        if (BizErpRules.isNetworkPriceOnlyProduct(canonical)) {
            if (row.getSalePrice() != null && row.getSalePrice().compareTo(BigDecimal.ZERO) != 0) {
                row.setSalePrice(BigDecimal.ZERO);
                return true;
            }
            if (row.getSalePrice() == null) {
                row.setSalePrice(BigDecimal.ZERO);
                return true;
            }
            return false;
        }
        BigDecimal fenyuan = BizErpRules.fenyuanSalePriceOf(canonical);
        if (fenyuan == null) {
            return false;
        }
        if (row.getSalePrice() == null || row.getSalePrice().compareTo(fenyuan) != 0) {
            row.setSalePrice(fenyuan);
            return true;
        }
        return false;
    }

    private static String defaultRemark(String canonical) {
        if (BizErpRules.isNetworkPriceOnlyProduct(canonical)) {
            return "仅摘取京东/天猫官方店网络实时价（不维护汾源销售价）";
        }
        BigDecimal p = BizErpRules.fenyuanSalePriceOf(canonical);
        if (p != null) {
            return "汾源销售价 " + p.toPlainString() + " 元/件（客户确认）；打开页面自动拉取线上价";
        }
        return "比价清单（打开页面自动拉取京东/天猫官方店价格）";
    }

    public BizPriceCompare getById(Long id) {
        BizPriceCompare entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizPriceCompare entity) {
        if (!StringUtils.hasText(entity.getSpec()) && StringUtils.hasText(entity.getProductName())) {
            entity.setSpec(BizErpRules.specOf(entity.getProductName()));
        }
        mapper.insert(entity);
    }

    public void update(Long id, BizPriceCompare entity) {
        getById(id);
        entity.setId(id);
        mapper.updateById(entity);
    }

    public void deleteByIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return;
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        if (!idList.isEmpty()) {
            mapper.deleteBatchIds(idList);
        }
    }
}
