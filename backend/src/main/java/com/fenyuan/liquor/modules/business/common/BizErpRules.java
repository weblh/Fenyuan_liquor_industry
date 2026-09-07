package com.fenyuan.liquor.modules.business.common;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 汾源经营体 ERP 业务规则（客户确认口径）。
 */
public final class BizErpRules {

    public static final String ERP_SYNC_MARK = "[GJP:";

    /** 销售/库存单据同步写入标记 */
    public static final String ERP_BILL_SYNC_MARK = "[GJP:BILL:";

    /** 异地销售统计：指定产品 */
    public static final List<String> OFFSITE_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            "42度青花20", "53度青花20", "53度老白汾10", "53度巴拿马20"
    ));

    /** 价格对比：指定产品 */
    public static final List<String> PRICE_COMPARE_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            "42度青花20", "53度青花20", "53度青花30", "53度老白汾10", "53度巴拿马20", "53度飞天茅台"
    ));

    /**
     * 汾源目前销售价（元/件），客户 2026-08-29 确认口径。
     * 53度飞天茅台不维护汾源价，仅摘取京东/天猫网络实时价。
     */
    private static final Map<String, BigDecimal> FENYUAN_SALE_PRICES = new LinkedHashMap<>();

    /** 仅摘取网络实时价、不维护汾源销售价的产品 */
    public static final List<String> NETWORK_PRICE_ONLY_PRODUCTS = Collections.unmodifiableList(Arrays.asList(
            "53度飞天茅台"
    ));

    /** 在线销售：看板展示名（美福 / 美团名酒行） */
    public static final List<String> ONLINE_SALE_CUSTOMERS = Collections.unmodifiableList(Arrays.asList(
            "美福", "美团名酒行"
    ));

    /**
     * 在线销售 ERP 客户别名 → 看板标准名。
     * 管家婆实际客户为「美福」「即时零售」（无「美团名酒行」主档名）。
     */
    private static final Map<String, String> ONLINE_SALE_ALIASES = new LinkedHashMap<>();

    /** 产品规格（ml），巴拿马20 为 475ml，其余 500ml */
    private static final Map<String, String> PRODUCT_SPECS = new LinkedHashMap<>();

    /** 产品 -> 系列 */
    private static final Map<String, String> PRODUCT_SERIES = new LinkedHashMap<>();

    /** 产品名模糊匹配关键词（canonical -> patterns） */
    private static final Map<String, List<String>> PRODUCT_KEYWORDS = new LinkedHashMap<>();

    /**
     * 看板/库存同步仓库字典（对应管家婆「存货库存详情」可选仓）：
     * 02=后库，05=门店前厅；其它仓（番禺库/花都库等）一律忽略。
     */
    public static final List<String> DASHBOARD_WAREHOUSES = Collections.unmodifiableList(Arrays.asList(
            "门店前厅", "后库"
    ));

    /** 前厅+后库汇总后的展示名 */
    public static final String COMBINED_WAREHOUSE_LABEL = "门店前厅+后库";

    /** 管家婆「按存货汇总 / 全部仓库」导入展示名 */
    public static final String ALL_WAREHOUSE_SUMMARY_LABEL = "全部仓库汇总";

    /** 管家婆仓库编号 -> 标准名称 */
    private static final Map<String, String> WAREHOUSE_CODE_DICT = new LinkedHashMap<>();

    /** 仓库别名/全名 -> 标准名称 */
    private static final Map<String, String> WAREHOUSE_ALIAS_DICT = new LinkedHashMap<>();

    /** 客户名称关键词 -> 省份（地址缺失时兜底，客户确认口径） */
    private static final Map<String, String> CUSTOMER_PROVINCE_HINTS = new LinkedHashMap<>();

    /** 地级市/地区 -> 省份 */
    private static final Map<String, String> CITY_TO_PROVINCE = new LinkedHashMap<>();

    /** 广州市辖区（含县级市） */
    private static final List<String> GUANGZHOU_DISTRICTS = Collections.unmodifiableList(Arrays.asList(
            "广州", "天河", "越秀", "荔湾", "海珠", "白云", "黄埔", "番禺", "花都", "南沙", "从化", "增城"
    ));

    static {
        for (String p : OFFSITE_PRODUCTS) {
            registerProduct(p);
        }
        registerProduct("53度青花30");
        registerProduct("53度飞天茅台");
        registerProduct("茅台1935");

        PRODUCT_KEYWORDS.put("42度青花20", Arrays.asList("42度青花20", "42 度青花 20", "42度青花 20", "42度青花20升级版"));
        PRODUCT_KEYWORDS.put("53度青花20", Arrays.asList("53度青花20", "53 度青花 20", "53度青花 20", "53度青花20升级版"));
        PRODUCT_KEYWORDS.put("53度青花30", Arrays.asList("53度青花30", "53 度青花 30", "青花30复兴"));
        PRODUCT_KEYWORDS.put("53度老白汾10", Arrays.asList("53度老白汾10", "53度老白汾酒10", "53度老白汾 10", "老白汾酒10"));
        PRODUCT_KEYWORDS.put("53度巴拿马20", Arrays.asList("53度巴拿马20", "53度巴拿马20汾酒", "42度巴拿马20"));
        PRODUCT_KEYWORDS.put("53度飞天茅台", Arrays.asList("53度飞天茅台", "飞天茅台"));

        FENYUAN_SALE_PRICES.put("53度青花20", new BigDecimal("2350"));
        FENYUAN_SALE_PRICES.put("42度青花20", new BigDecimal("2300"));
        FENYUAN_SALE_PRICES.put("53度巴拿马20", new BigDecimal("1820"));
        FENYUAN_SALE_PRICES.put("53度老白汾10", new BigDecimal("810"));

        // 长词优先：即时零售/美团* → 美团名酒行；美福 → 美福
        ONLINE_SALE_ALIASES.put("美团名酒行", "美团名酒行");
        ONLINE_SALE_ALIASES.put("即时零售", "美团名酒行");
        ONLINE_SALE_ALIASES.put("美团", "美团名酒行");
        ONLINE_SALE_ALIASES.put("美福", "美福");

        // 客户确认：公司名称映射到属地省份（地址为空时）
        CUSTOMER_PROVINCE_HINTS.put("华翔", "山西");
        CUSTOMER_PROVINCE_HINTS.put("美的智慧", "广东");
        CUSTOMER_PROVINCE_HINTS.put("美的", "广东");
        CUSTOMER_PROVINCE_HINTS.put("美福", "广东");
        CUSTOMER_PROVINCE_HINTS.put("即时零售", "广东");

        CITY_TO_PROVINCE.put("临汾", "山西");
        CITY_TO_PROVINCE.put("太原", "山西");
        CITY_TO_PROVINCE.put("大同", "山西");
        CITY_TO_PROVINCE.put("运城", "山西");
        CITY_TO_PROVINCE.put("吕梁", "山西");
        CITY_TO_PROVINCE.put("晋中", "山西");
        CITY_TO_PROVINCE.put("洪洞", "山西");
        CITY_TO_PROVINCE.put("翼城", "山西");
        CITY_TO_PROVINCE.put("广州", "广东");
        CITY_TO_PROVINCE.put("佛山", "广东");
        CITY_TO_PROVINCE.put("顺德", "广东");
        CITY_TO_PROVINCE.put("中山", "广东");
        CITY_TO_PROVINCE.put("深圳", "广东");
        CITY_TO_PROVINCE.put("珠海", "广东");
        CITY_TO_PROVINCE.put("东莞", "广东");
        CITY_TO_PROVINCE.put("番禺", "广东");
        CITY_TO_PROVINCE.put("花都", "广东");
        CITY_TO_PROVINCE.put("北京", "北京");
        CITY_TO_PROVINCE.put("上海", "上海");
        CITY_TO_PROVINCE.put("天津", "天津");
        CITY_TO_PROVINCE.put("重庆", "重庆");
        CITY_TO_PROVINCE.put("杭州", "浙江");
        CITY_TO_PROVINCE.put("宁波", "浙江");
        CITY_TO_PROVINCE.put("南京", "江苏");
        CITY_TO_PROVINCE.put("苏州", "江苏");
        CITY_TO_PROVINCE.put("常州", "江苏");
        CITY_TO_PROVINCE.put("青岛", "山东");
        CITY_TO_PROVINCE.put("济南", "山东");
        CITY_TO_PROVINCE.put("合肥", "安徽");
        CITY_TO_PROVINCE.put("芜湖", "安徽");
        CITY_TO_PROVINCE.put("西安", "陕西");
        CITY_TO_PROVINCE.put("兰州", "甘肃");
        CITY_TO_PROVINCE.put("郑州", "河南");
        CITY_TO_PROVINCE.put("武汉", "湖北");
        CITY_TO_PROVINCE.put("成都", "四川");

        // 管家婆仓库字典（业务报表 > 库存报表 > 存货库存详情）
        WAREHOUSE_CODE_DICT.put("02", "后库");
        WAREHOUSE_CODE_DICT.put("05", "门店前厅");
        WAREHOUSE_ALIAS_DICT.put("后库", "后库");
        WAREHOUSE_ALIAS_DICT.put("门店前厅", "门店前厅");
        WAREHOUSE_ALIAS_DICT.put("前厅", "门店前厅");
        WAREHOUSE_ALIAS_DICT.put("店前厅", "门店前厅");
    }

    private BizErpRules() {
    }

    private static void registerProduct(String canonical) {
        if (canonical.contains("茅台")) {
            PRODUCT_SERIES.put(canonical, "茅台系列");
        } else {
            PRODUCT_SERIES.put(canonical, "汾酒系列");
        }
        if (canonical.contains("巴拿马20")) {
            PRODUCT_SPECS.put(canonical, "475ml");
        } else {
            PRODUCT_SPECS.put(canonical, "500ml");
        }
    }

    public static String specOf(String canonicalProduct) {
        return PRODUCT_SPECS.getOrDefault(canonicalProduct, "500ml");
    }

    public static String seriesOf(String canonicalProduct) {
        return PRODUCT_SERIES.getOrDefault(canonicalProduct, "汾酒系列");
    }

    /** 汾源确认销售价（元/件）；飞天茅台等仅网络价产品返回 null */
    public static BigDecimal fenyuanSalePriceOf(String canonicalProduct) {
        if (!StringUtils.hasText(canonicalProduct) || isNetworkPriceOnlyProduct(canonicalProduct)) {
            return null;
        }
        return FENYUAN_SALE_PRICES.get(canonicalProduct);
    }

    public static boolean isNetworkPriceOnlyProduct(String canonicalProduct) {
        return StringUtils.hasText(canonicalProduct) && NETWORK_PRICE_ONLY_PRODUCTS.contains(canonicalProduct);
    }

    public static boolean isErpSynced(String remark) {
        return StringUtils.hasText(remark) && remark.startsWith(ERP_SYNC_MARK);
    }

    public static boolean matchesCanonicalProduct(String productName, String canonical) {
        if (!StringUtils.hasText(productName) || !StringUtils.hasText(canonical)) {
            return false;
        }
        String normalized = normalize(productName);
        List<String> keywords = PRODUCT_KEYWORDS.get(canonical);
        if (keywords != null) {
            for (String kw : keywords) {
                if (normalized.contains(normalize(kw))) {
                    return true;
                }
            }
        }
        return normalized.contains(normalize(canonical));
    }

    public static boolean matchesAnyOffsiteProduct(String productName) {
        return OFFSITE_PRODUCTS.stream().anyMatch(p -> matchesCanonicalProduct(productName, p));
    }

    public static boolean matchesAnyPriceCompareProduct(String productName) {
        return PRICE_COMPARE_PRODUCTS.stream().anyMatch(p -> matchesCanonicalProduct(productName, p));
    }

    public static String resolveCanonicalProduct(String productName, List<String> candidates) {
        if (!StringUtils.hasText(productName)) {
            return null;
        }
        for (String c : candidates) {
            if (matchesCanonicalProduct(productName, c)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 收货地址是否在广州市行政区域以外（含非广东省、省内非广州）。
     */
    public static boolean isOutsideGuangzhou(String province, String city, String address) {
        String text = normalize(province) + normalize(city) + normalize(address);
        if (!text.contains("广东") && !text.contains("广州") && !text.contains("粤")) {
            // 非广东地址视为异地
            if (StringUtils.hasText(province) && !normalize(province).contains("广州")) {
                return true;
            }
        }
        for (String district : GUANGZHOU_DISTRICTS) {
            if (text.contains(normalize(district))) {
                return false;
            }
        }
        // 广东省内但不含广州辖区
        if (text.contains("广东") || text.contains("粤")) {
            return !text.contains("广州");
        }
        return StringUtils.hasText(province) || StringUtils.hasText(city) || StringUtils.hasText(address);
    }

    public static boolean isOnlineSaleCustomer(String customerName) {
        return StringUtils.hasText(resolveOnlineSaleCustomer(customerName));
    }

    /**
     * 将 ERP 客户名解析为在线销售标准名（美福 / 美团名酒行）；非在线渠道返回 null。
     */
    public static String resolveOnlineSaleCustomer(String customerName) {
        if (!StringUtils.hasText(customerName)) {
            return null;
        }
        String n = customerName.trim();
        // 长词优先，避免「美的」等误伤（别名表不含美的）
        for (Map.Entry<String, String> e : ONLINE_SALE_ALIASES.entrySet()) {
            String alias = e.getKey();
            if (n.equals(alias) || n.contains(alias)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 销售产品结构统计：所有已注册系列产品 */
    public static List<String> structureProductCandidates() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(PRODUCT_SERIES.keySet()));
    }

    public static boolean matchesAnyStructureProduct(String productName) {
        return structureProductCandidates().stream().anyMatch(p -> matchesCanonicalProduct(productName, p));
    }

    /** 从主档同步 remark 中解析客户地址 */
    public static String parseAddressFromRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return "";
        }
        String marker = "管家婆客户同步";
        int idx = remark.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        String tail = remark.substring(idx + marker.length()).trim();
        return tail.isEmpty() ? "" : tail;
    }

    /**
     * 综合客户名称 + 收货地址解析为 [省, 市]。
     * 优先地址；地址缺失时从公司名称/城市关键词推断（如华翔集团→山西、美的智慧家居→广东）。
     */
    public static String[] resolveRegion(String customerName, String address) {
        String[] fromAddr = parseProvinceCity(address);
        if (StringUtils.hasText(fromAddr[0])) {
            return normalizeProvinceCity(fromAddr);
        }
        String[] fromName = parseProvinceCity(customerName);
        if (StringUtils.hasText(fromName[0])) {
            return normalizeProvinceCity(fromName);
        }
        String hint = inferProvinceHint(customerName, address);
        if (StringUtils.hasText(hint)) {
            return new String[]{hint, fromName[1]};
        }
        return new String[]{"", ""};
    }

    /**
     * 简易地址/名称解析为 [省, 市]。
     */
    public static String[] parseProvinceCity(String address) {
        String text = address == null ? "" : address.trim();
        if (!StringUtils.hasText(text)) {
            return new String[]{"", ""};
        }
        String province = "";
        String city = "";
        if (text.startsWith("内蒙古")) {
            province = "内蒙古";
            text = text.substring(3);
        } else if (text.startsWith("黑龙江")) {
            province = "黑龙江";
            text = text.substring(3);
        } else {
            int pIdx = text.indexOf('省');
            if (pIdx > 0 && pIdx <= 3) {
                province = text.substring(0, pIdx);
                text = text.substring(pIdx + 1);
            } else if (text.startsWith("北京") || text.startsWith("上海")
                    || text.startsWith("天津") || text.startsWith("重庆")) {
                province = text.substring(0, 2);
                text = text.substring(2);
            } else if (text.startsWith("广东") || text.startsWith("山西") || text.startsWith("山东")
                    || text.startsWith("河南") || text.startsWith("河北") || text.startsWith("江苏")
                    || text.startsWith("浙江") || text.startsWith("安徽") || text.startsWith("福建")
                    || text.startsWith("江西") || text.startsWith("湖北") || text.startsWith("湖南")
                    || text.startsWith("四川") || text.startsWith("贵州") || text.startsWith("云南")
                    || text.startsWith("陕西") || text.startsWith("甘肃") || text.startsWith("青海")
                    || text.startsWith("辽宁") || text.startsWith("吉林") || text.startsWith("广西")
                    || text.startsWith("海南") || text.startsWith("宁夏") || text.startsWith("新疆")
                    || text.startsWith("西藏")) {
                province = text.substring(0, 2);
                text = text.substring(2);
            }
        }
        int cIdx = text.indexOf('市');
        if (cIdx > 0 && cIdx <= 4) {
            city = text.substring(0, cIdx);
        }
        if (!StringUtils.hasText(province)) {
            province = inferProvinceHint(null, address);
        }
        return normalizeProvinceCity(new String[]{province, city});
    }

    /** 是否看板同步仓库（仅 02后库 / 05门店前厅） */
    public static boolean isDashboardWarehouse(String warehouse) {
        return StringUtils.hasText(resolveDashboardWarehouse(warehouse));
    }

    /**
     * 将管家婆仓库编号或名称解析为标准仓名；非字典仓返回 null。
     * 支持：02 / 05 / 后库 / 门店前厅 / 前厅 / 含上述关键字的全名。
     */
    public static String resolveDashboardWarehouse(String warehouseOrCode) {
        if (!StringUtils.hasText(warehouseOrCode)) {
            return null;
        }
        String w = warehouseOrCode.trim();
        if (w.contains("代销") || w.contains("番禺") || w.contains("花都") || w.contains("临汾")
                || w.contains("顺德") || w.contains("北京")) {
            return null;
        }
        String byCode = WAREHOUSE_CODE_DICT.get(w);
        if (byCode != null) {
            return byCode;
        }
        // 编号夹在全名中，如 "05门店前厅"
        for (Map.Entry<String, String> e : WAREHOUSE_CODE_DICT.entrySet()) {
            if (w.equals(e.getKey()) || w.startsWith(e.getKey()) || w.contains("|" + e.getKey())) {
                return e.getValue();
            }
        }
        for (Map.Entry<String, String> e : WAREHOUSE_ALIAS_DICT.entrySet()) {
            if (w.equals(e.getKey()) || w.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String inferProvinceHint(String customerName, String address) {
        String text = (customerName == null ? "" : customerName) + (address == null ? "" : address);
        if (!StringUtils.hasText(text)) {
            return "";
        }
        for (Map.Entry<String, String> e : CUSTOMER_PROVINCE_HINTS.entrySet()) {
            if (text.contains(e.getKey())) {
                return e.getValue();
            }
        }
        for (Map.Entry<String, String> e : CITY_TO_PROVINCE.entrySet()) {
            if (text.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return "";
    }

    private static String[] normalizeProvinceCity(String[] region) {
        String province = region[0] == null ? "" : region[0].replace("省", "").replace("市", "").trim();
        String city = region[1] == null ? "" : region[1].replace("市", "").trim();
        if ("广州".equals(city) && !StringUtils.hasText(province)) {
            province = "广东";
        }
        return new String[]{province, city};
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Pattern.compile("[\\s　]+").matcher(s.toLowerCase()).replaceAll("");
    }
}
