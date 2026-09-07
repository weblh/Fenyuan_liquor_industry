package com.fenyuan.liquor.modules.integration.cmcloud.service;

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
import com.fenyuan.liquor.modules.business.onlinesale.entity.BizOnlineSale;
import com.fenyuan.liquor.modules.business.onlinesale.mapper.BizOnlineSaleMapper;
import com.fenyuan.liquor.modules.business.pricecompare.entity.BizPriceCompare;
import com.fenyuan.liquor.modules.business.pricecompare.mapper.BizPriceCompareMapper;
import com.fenyuan.liquor.modules.business.productstructure.entity.BizProductStructure;
import com.fenyuan.liquor.modules.business.productstructure.mapper.BizProductStructureMapper;
import com.fenyuan.liquor.modules.business.salesrank.entity.BizSalesRank;
import com.fenyuan.liquor.modules.business.salesrank.mapper.BizSalesRankMapper;
import com.fenyuan.liquor.modules.integration.cmcloud.dto.CmCloudSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmCloudSyncService {

    private static final String MARK = BizErpRules.ERP_SYNC_MARK;

    private static final List<String> DEMO_CUSTOMERS = Arrays.asList(
            "运城名酒汇", "晋中商贸有限公司", "吕梁烟酒行"
    );

    private static final List<String> DEMO_DEALERS = Arrays.asList(
            "太原经销商", "大同经销商", "临汾经销商"
    );

    private final CmCloudProperties properties;
    private final BizCustomerMaintainMapper customerMaintainMapper;
    private final BizInventoryMapper inventoryMapper;
    private final BizProductStructureMapper productStructureMapper;
    private final BizPriceCompareMapper priceCompareMapper;
    private final BizCustomerDevMapper customerDevMapper;
    private final BizSalesRankMapper salesRankMapper;
    private final BizOnlineSaleMapper onlineSaleMapper;

    @Transactional(rollbackFor = Exception.class)
    public CmCloudSyncResult syncMasterData() {
        if (!properties.isSyncEnabled()) {
            throw new BusinessException("管家婆同步已禁用");
        }
        Path db = findBaseInfoDb();
        List<CustomerRow> customers = new ArrayList<>();
        List<ProductRow> products = new ArrayList<>();
        String defaultWarehouse = "门店前厅";

        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath())) {
                loadCustomers(conn, customers);
                loadProducts(conn, products);
                defaultWarehouse = loadPreferredWarehouse(conn, defaultWarehouse);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取管家婆 BaseInfo 失败", e);
            throw new BusinessException("读取管家婆缓存失败: " + e.getMessage());
        }

        clearDemoData();
        clearPreviousSync();

        int custN = 0;
        for (CustomerRow c : customers) {
            String remark = MARK + c.code + "] 管家婆客户同步"
                    + (StringUtils.hasText(c.addr) ? " " + c.addr : "");
            BizCustomerMaintain m = new BizCustomerMaintain();
            m.setCustomerName(c.name);
            m.setContactPhone(StringUtils.hasText(c.phone) ? c.phone : null);
            m.setLastPurchaseDate(null);
            m.setDaysSincePurchase(null);
            m.setAlertStatus(0);
            m.setRemark(remark);
            customerMaintainMapper.insert(m);
            custN++;
        }

        // 库存由单据同步（GoodsStocks）按门店前厅/后库写入，避免主档灌入全量 0 且仓库落成番禺库
        int invN = 0;

        int priceN = syncPriceCompareProducts(products);
        int devN = syncNewCustomers();

        return CmCloudSyncResult.builder()
                .sourceDb(db.toString())
                .customers(custN)
                .inventory(invN)
                .productStructure(0)
                .priceCompare(priceN)
                .customerDev(devN)
                .defaultWarehouse(defaultWarehouse)
                .build();
    }

    private int syncPriceCompareProducts(List<ProductRow> products) {
        int n = 0;
        for (String canonical : BizErpRules.PRICE_COMPARE_PRODUCTS) {
            BigDecimal erpPrice = BigDecimal.ZERO;
            String spec = BizErpRules.specOf(canonical);
            for (ProductRow p : products) {
                if (BizErpRules.matchesCanonicalProduct(p.name, canonical)) {
                    if (p.price != null && p.price.compareTo(BigDecimal.ZERO) > 0) {
                        erpPrice = p.price;
                    }
                    break;
                }
            }
            // 客户确认汾源价优先；飞天茅台仅网络价
            BigDecimal fenyuan = BizErpRules.fenyuanSalePriceOf(canonical);
            boolean networkOnly = BizErpRules.isNetworkPriceOnlyProduct(canonical);
            BigDecimal salePrice = networkOnly ? BigDecimal.ZERO
                    : (fenyuan != null ? fenyuan : erpPrice);

            BizPriceCompare existing = priceCompareMapper.selectOne(new LambdaQueryWrapper<BizPriceCompare>()
                    .eq(BizPriceCompare::getProductName, canonical)
                    .last("LIMIT 1"));
            if (existing == null) {
                existing = priceCompareMapper.selectList(new LambdaQueryWrapper<>()).stream()
                        .filter(e -> BizErpRules.matchesCanonicalProduct(e.getProductName(), canonical))
                        .findFirst()
                        .orElse(null);
            }
            String remark;
            if (networkOnly) {
                remark = MARK + "PRICE:" + canonical + "] 仅摘取京东/天猫网络实时价";
            } else if (fenyuan != null) {
                remark = MARK + "PRICE:" + canonical + "] 汾源销售价 " + fenyuan.toPlainString() + " 元/件";
            } else {
                remark = MARK + "PRICE:" + canonical + "] 比价产品"
                        + (salePrice.compareTo(BigDecimal.ZERO) > 0 ? "（ERP零售价）" : "（官方店铺价待维护）");
            }
            if (existing != null) {
                existing.setProductName(canonical);
                existing.setSpec(spec);
                existing.setSalePrice(salePrice);
                // 保留已维护的京东/天猫价与抓取时间
                existing.setRemark(remark);
                priceCompareMapper.updateById(existing);
            } else {
                BizPriceCompare row = new BizPriceCompare();
                row.setProductName(canonical);
                row.setSpec(spec);
                row.setSalePrice(salePrice);
                row.setRemark(remark);
                priceCompareMapper.insert(row);
            }
            n++;
        }
        return n;
    }

    /**
     * 客户开发改由销售单据同步按「历史无销售 + 当月首单」写入，主档同步不再灌种子数据。
     */
    private int syncNewCustomers() {
        // 清理历史硬编码种子，保留单据同步写入的 [GJP:BILL: 记录
        customerDevMapper.delete(new LambdaQueryWrapper<BizCustomerDev>()
                .likeRight(BizCustomerDev::getRemark, MARK)
                .notLike(BizCustomerDev::getRemark, BizErpRules.ERP_BILL_SYNC_MARK));
        return 0;
    }

    private void clearDemoData() {
        for (String name : DEMO_CUSTOMERS) {
            customerMaintainMapper.delete(new LambdaQueryWrapper<BizCustomerMaintain>()
                    .like(BizCustomerMaintain::getCustomerName, name));
            customerDevMapper.delete(new LambdaQueryWrapper<BizCustomerDev>()
                    .like(BizCustomerDev::getName, name));
        }
        for (String name : DEMO_DEALERS) {
            salesRankMapper.delete(new LambdaQueryWrapper<BizSalesRank>()
                    .eq(BizSalesRank::getCompanyName, name));
        }
        inventoryMapper.delete(new LambdaQueryWrapper<BizInventory>()
                .and(w -> w.like(BizInventory::getProductName, "汾源原浆")
                        .or().like(BizInventory::getProductName, "汾源陈酿")
                        .or().like(BizInventory::getProductName, "汾源礼盒")
                        .or().like(BizInventory::getWarehouse, "番禺")
                        .or().eq(BizInventory::getWarehouse, "主库")));
        productStructureMapper.delete(new LambdaQueryWrapper<BizProductStructure>()
                .and(w -> w.like(BizProductStructure::getCategory, "原浆")
                        .or().like(BizProductStructure::getCategory, "陈酿")
                        .or().like(BizProductStructure::getCategory, "礼盒")));
        onlineSaleMapper.delete(new LambdaQueryWrapper<BizOnlineSale>()
                .isNull(BizOnlineSale::getCustomerName));
    }

    private void clearPreviousSync() {
        customerMaintainMapper.delete(new LambdaQueryWrapper<BizCustomerMaintain>()
                .likeRight(BizCustomerMaintain::getRemark, MARK));
        // 库存由单据同步（前厅/后库）维护，主档同步不清理库存表
        productStructureMapper.delete(new LambdaQueryWrapper<BizProductStructure>()
                .likeRight(BizProductStructure::getRemark, MARK)
                .notLike(BizProductStructure::getRemark, BizErpRules.ERP_BILL_SYNC_MARK));
    }

    private Path findBaseInfoDb() {
        Path root = Paths.get(properties.getBaseInfoPath());
        if (!Files.isDirectory(root)) {
            throw new BusinessException("管家婆 BaseInfo 目录不存在: " + root);
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isSqliteFile)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("未找到管家婆 BaseInfo SQLite 文件"));
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("扫描管家婆 BaseInfo 失败: " + e.getMessage());
        }
    }

    private boolean isSqliteFile(Path path) {
        try (java.io.InputStream in = Files.newInputStream(path)) {
            byte[] head = new byte[15];
            int n = in.read(head);
            return n == 15 && "SQLite format 3".equals(new String(head));
        } catch (IOException e) {
            return false;
        }
    }

    private void loadCustomers(Connection conn, List<CustomerRow> out) throws Exception {
        String sql = "SELECT bcbusercode, bcBNAME, IFNULL(bcMobile,''), IFNULL(bcmophone,''), IFNULL(bctelandaddress,'') "
                + "FROM C WHERE IFNULL(bBCIsStop,0)=0 AND IFNULL(bcbusercode,'')<>''";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = trim(rs.getString(1));
                String name = trim(rs.getString(2));
                if (!isLeafCode(code) || !StringUtils.hasText(name) || "客户".equals(name) || "供应商".equals(name)) {
                    continue;
                }
                String mobile = trim(rs.getString(3));
                String phone = trim(rs.getString(4));
                CustomerRow row = new CustomerRow();
                row.code = code;
                row.name = name;
                row.phone = StringUtils.hasText(mobile) ? mobile : phone;
                row.addr = trim(rs.getString(5));
                out.add(row);
            }
        }
    }

    private void loadProducts(Connection conn, List<ProductRow> out) throws Exception {
        String sql = "SELECT PUSERCODE, PNAME, IFNULL(STANDARD,''), IFNULL(UNIT1,''), IFNULL(RetailPrice,0), IFNULL(TYPE,'') "
                + "FROM P WHERE IFNULL(bPIsStop,0)=0 AND IFNULL(PUSERCODE,'')<>''";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = trim(rs.getString(1));
                String name = trim(rs.getString(2));
                if (!isLeafCode(code) || !StringUtils.hasText(name)) {
                    continue;
                }
                if ("01".equalsIgnoreCase(code) || "fenju".equalsIgnoreCase(code) || "汾酒类".equals(name)) {
                    continue;
                }
                ProductRow row = new ProductRow();
                row.code = code;
                row.name = name;
                row.spec = trim(rs.getString(3));
                row.unit = trim(rs.getString(4));
                row.price = rs.getBigDecimal(5);
                row.type = trim(rs.getString(6));
                out.add(row);
            }
        }
    }

    /** 优先门店前厅 / 后库，避免默认落成番禺库 */
    private String loadPreferredWarehouse(Connection conn, String fallback) throws Exception {
        String sql = "SELECT KUSERCODE, IFNULL(KNAME,''), IFNULL(KFULLNAME,'') FROM K "
                + "WHERE IFNULL(bKIsStop,0)=0 AND IFNULL(KUSERCODE,'')<>''";
        String foundFront = null;
        String foundBack = null;
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String full = trim(rs.getString(3));
                String name = trim(rs.getString(2));
                String label = StringUtils.hasText(full) ? full : name;
                if (!StringUtils.hasText(label) || label.contains("代销")) {
                    continue;
                }
                if (BizErpRules.isDashboardWarehouse(label)) {
                    if (label.contains("前厅") && foundFront == null) {
                        foundFront = "门店前厅";
                    } else if (label.contains("后库") && foundBack == null) {
                        foundBack = "后库";
                    }
                }
            }
        }
        if (foundFront != null) {
            return foundFront;
        }
        if (foundBack != null) {
            return foundBack;
        }
        return fallback;
    }

    private boolean isLeafCode(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if ("01".equals(code) || "00".equals(code) || "00000".equals(code)) {
            return false;
        }
        return !(code.startsWith("01") && code.length() <= 2);
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static class CustomerRow {
        String code;
        String name;
        String phone;
        String addr;
    }

    private static class ProductRow {
        String code;
        String name;
        String spec;
        String unit;
        BigDecimal price;
        String type;
    }
}
