package com.fenyuan.liquor.modules.integration.cmcloud.service;

import com.alibaba.excel.EasyExcel;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.modules.business.common.BizErpRules;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 解析管家婆报表导出的 Excel（销售明细表 / 存货库存详情），按中文表头模糊匹配列。
 */
final class CmCloudReportExcelReader {

    private CmCloudReportExcelReader() {
    }

    static List<SaleRow> readSales(InputStream in) {
        List<Map<Integer, String>> raw = readRaw(in);
        int headIdx = findHeaderRow(raw, "客户", "存货", "商品", "品名", "数量", "金额");
        if (headIdx < 0) {
            throw new BusinessException("未识别销售明细表头，请导出管家婆「销售明细表」后上传");
        }
        Map<String, Integer> cols = mapColumns(raw.get(headIdx));
        Integer customerCol = firstCol(cols, "客户全名", "客户名称", "往来单位", "单位全名", "客户");
        Integer productCol = firstCol(cols, "存货全名", "商品全名", "品名", "存货名称", "商品名称", "存货");
        Integer qtyCol = firstCol(cols, "基本数量", "数量", "销售数量", "辅助数量");
        Integer amountCol = firstCol(cols, "价税合计", "金额", "销售金额", "合计", "总金额");
        Integer dateCol = firstCol(cols, "日期", "单据日期", "业务日期", "开单日期");
        Integer warehouseCol = firstCol(cols, "仓库全名", "仓库名称", "出库仓库", "仓库");
        Integer typeCol = firstCol(cols, "单据类型", "业务类型", "类型");
        Integer billNoCol = firstCol(cols, "单据编号", "单号", "编号");

        if (customerCol == null || productCol == null) {
            throw new BusinessException("销售明细缺少「客户」或「存货/商品」列");
        }

        List<SaleRow> rows = new ArrayList<>();
        for (int i = headIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            String customer = cell(line, customerCol);
            String product = cell(line, productCol);
            if (!StringUtils.hasText(customer) && !StringUtils.hasText(product)) {
                continue;
            }
            if (!StringUtils.hasText(customer) || !StringUtils.hasText(product)) {
                continue;
            }
            if (isTotalRow(customer) || isTotalRow(product)) {
                continue;
            }
            SaleRow row = new SaleRow();
            row.customer = customer.trim();
            row.productName = product.trim();
            row.quantity = parseDecimal(cell(line, qtyCol));
            row.amount = parseDecimal(cell(line, amountCol));
            row.billDate = parseDate(cell(line, dateCol));
            row.warehouse = cell(line, warehouseCol);
            row.billNo = cell(line, billNoCol);
            String type = cell(line, typeCol);
            row.refund = StringUtils.hasText(type) && (type.contains("退") || type.contains("红字"));
            if (row.quantity == null && row.amount == null) {
                continue;
            }
            if (row.quantity == null) {
                row.quantity = BigDecimal.ZERO;
            }
            if (row.amount == null) {
                row.amount = BigDecimal.ZERO;
            }
            if (row.refund) {
                row.quantity = row.quantity.abs().negate();
                row.amount = row.amount.abs().negate();
            }
            rows.add(row);
        }
        return rows;
    }

    static List<StockRow> readStocks(InputStream in) {
        List<Map<Integer, String>> raw = readRaw(in);
        // 兼容两种：分仓库明细（含仓库列）、按存货汇总（无仓库列，全仓已合计）
        int headIdx = findHeaderRow(raw, 2, "存货", "商品", "数量");
        if (headIdx < 0) {
            headIdx = findHeaderRow(raw, 2, "仓库", "存货", "数量");
        }
        if (headIdx < 0) {
            throw new BusinessException("未识别库存表头，请导出管家婆「存货库存详情」或勾选「按存货汇总」后上传");
        }
        Map<String, Integer> cols = mapColumns(raw.get(headIdx));
        Integer warehouseCol = firstCol(cols, "仓库全名", "仓库名称", "仓库编号", "仓库");
        Integer productCol = firstCol(cols, "存货全名", "商品全名", "品名", "存货名称", "商品名称", "存货");
        // 优先基本「数量」，避免误匹配「辅1数量/辅助数量」
        Integer qtyCol = exactCol(cols, "数量", "基本数量", "结存数量");
        if (qtyCol == null) {
            qtyCol = firstCol(cols, "数量", "基本数量", "结存数量");
        }
        Integer codeCol = firstCol(cols, "存货编号", "商品编号", "编号");
        Integer specCol = firstCol(cols, "规格", "型号", "存货规格");
        Integer priceCol = firstCol(cols, "单价", "成本价", "零售价", "含税单价");
        boolean summaryByProduct = warehouseCol == null;

        if (productCol == null || qtyCol == null) {
            throw new BusinessException("库存明细缺少「存货全名」或「数量」列");
        }

        List<StockRow> rows = new ArrayList<>();
        for (int i = headIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            String product = cell(line, productCol);
            if (!StringUtils.hasText(product) || isTotalRow(product)) {
                continue;
            }
            BigDecimal qty = parseDecimal(cell(line, qtyCol));
            if (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            StockRow row = new StockRow();
            row.productName = product.trim();
            row.productCode = cell(line, codeCol);
            row.warehouse = summaryByProduct ? BizErpRules.ALL_WAREHOUSE_SUMMARY_LABEL : cell(line, warehouseCol);
            row.summaryByProduct = summaryByProduct;
            row.quantity = qty;
            row.spec = cell(line, specCol);
            row.unitPrice = parseDecimal(cell(line, priceCol));
            rows.add(row);
        }
        return rows;
    }

    private static List<Map<Integer, String>> readRaw(InputStream in) {
        List<Map<Integer, String>> raw = EasyExcel.read(in)
                .autoTrim(true)
                .sheet()
                .headRowNumber(0)
                .doReadSync();
        return raw == null ? new ArrayList<>() : raw;
    }

    private static int findHeaderRow(List<Map<Integer, String>> raw, String... keywords) {
        return findHeaderRow(raw, 2, keywords);
    }

    private static int findHeaderRow(List<Map<Integer, String>> raw, int minHits, String... keywords) {
        int limit = Math.min(raw.size(), 30);
        for (int i = 0; i < limit; i++) {
            String joined = String.join("|", mapColumns(raw.get(i)).keySet());
            int hit = 0;
            for (String kw : keywords) {
                if (joined.contains(kw)) {
                    hit++;
                }
            }
            if (hit >= minHits) {
                return i;
            }
        }
        return -1;
    }

    private static Integer exactCol(Map<String, Integer> cols, String... names) {
        for (String name : names) {
            String key = name.replace(" ", "");
            if (cols.containsKey(key)) {
                return cols.get(key);
            }
        }
        return null;
    }

    private static Map<String, Integer> mapColumns(Map<Integer, String> row) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (row == null) {
            return map;
        }
        for (Map.Entry<Integer, String> e : row.entrySet()) {
            if (!StringUtils.hasText(e.getValue())) {
                continue;
            }
            map.put(e.getValue().trim().replace(" ", ""), e.getKey());
        }
        return map;
    }

    private static Integer firstCol(Map<String, Integer> cols, String... names) {
        for (String name : names) {
            String key = name.replace(" ", "");
            if (cols.containsKey(key)) {
                return cols.get(key);
            }
            for (Map.Entry<String, Integer> e : cols.entrySet()) {
                if (e.getKey().contains(key) || key.contains(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private static String cell(Map<Integer, String> row, Integer col) {
        if (row == null || col == null) {
            return null;
        }
        String v = row.get(col);
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private static boolean isTotalRow(String text) {
        String t = text.replace(" ", "");
        return t.contains("合计") || t.contains("小计") || t.contains("总计") || "合计".equals(t);
    }

    private static BigDecimal parseDecimal(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String t = text.trim()
                .replace(",", "")
                .replace("，", "")
                .replace("￥", "")
                .replace("元", "")
                .replace("%", "");
        if (t.isEmpty() || "-".equals(t)) {
            return null;
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String t = text.trim().replace("/", "-").replace(".", "-");
        if (t.length() >= 10) {
            t = t.substring(0, 10);
        }
        DateTimeFormatter[] fmts = {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
        };
        for (DateTimeFormatter fmt : fmts) {
            try {
                return LocalDate.parse(t, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    static final class SaleRow {
        String customer;
        String productName;
        String warehouse;
        String billNo;
        LocalDate billDate;
        BigDecimal quantity;
        BigDecimal amount;
        boolean refund;
    }

    static final class StockRow {
        String productName;
        String productCode;
        String warehouse;
        String spec;
        BigDecimal quantity;
        BigDecimal unitPrice;
        /** true=管家婆「按存货汇总」（全仓已按品名合计，无仓库列） */
        boolean summaryByProduct;
    }
}
