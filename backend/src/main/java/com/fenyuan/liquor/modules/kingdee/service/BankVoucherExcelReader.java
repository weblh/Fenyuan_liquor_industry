package com.fenyuan.liquor.modules.kingdee.service;

import com.alibaba.excel.EasyExcel;
import com.fenyuan.liquor.modules.kingdee.dto.excel.KingdeeBankVoucherDetailExcel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多银行明细 Excel 读取：按渠道模板解析，归一化为统一明细行。
 */
@Slf4j
public final class BankVoucherExcelReader {

    public static final String TEMPLATE_CCB = "CCB_DETAIL";
    public static final String TEMPLATE_ICBC_RECEIPT = "ICBC_RECEIPT";
    public static final String TEMPLATE_ICBC_HISTORY = "ICBC_HISTORY";
    public static final String TEMPLATE_BOC = "BOC_DETAIL";
    /** 农商行客户电子回单（微信/支付宝待结算入账等） */
    public static final String TEMPLATE_RCB = "RCB_RECEIPT";

    private static final Pattern YUAN_AMOUNT = Pattern.compile(
            "[￥¥]\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*元?");
    private static final Pattern PLAIN_AMOUNT = Pattern.compile(
            "(?:票面金额|实收金额|金额|发生额)[:：]?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*元?");

    private BankVoucherExcelReader() {
    }

    public static List<KingdeeBankVoucherDetailExcel> read(InputStream inputStream) {
        return read(inputStream, null);
    }

    public static List<KingdeeBankVoucherDetailExcel> read(InputStream inputStream,
                                                           KingdeeBankChannel channel) {
        List<Map<Integer, String>> raw = EasyExcel.read(inputStream)
                .autoTrim(true)
                .sheet()
                .headRowNumber(0)
                .doReadSync();
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }

        String detected = detectTemplate(raw);
        String configured = channel != null && StringUtils.hasText(channel.getExcelTemplate())
                ? channel.getExcelTemplate().trim()
                : null;
        String template = resolveTemplate(configured, detected, channel);
        log.info("银行明细解析模板={} detected={} configured={} channel={}", template, detected, configured,
                channel != null ? channel.getChannelKey() : null);

        switch (template) {
            case TEMPLATE_ICBC_RECEIPT:
                return readIcbcReceipt(raw, channel);
            case TEMPLATE_ICBC_HISTORY:
                return readIcbcHistory(raw, channel);
            case TEMPLATE_BOC:
                return readBoc(raw, channel);
            case TEMPLATE_RCB:
                return readRcb(raw, channel);
            case TEMPLATE_CCB:
            default:
                return readCcb(raw, channel);
        }
    }

    static String detectTemplate(List<Map<Integer, String>> raw) {
        String joined = joinFirstRows(raw, 12);
        if (joined.contains("电子回单号码") && joined.contains("付方账号")) {
            return TEMPLATE_ICBC_RECEIPT;
        }
        // 农商行电子回单：客户电子回单信息 / 支出/存入 / 对方账号名称
        if ((joined.contains("客户电子回单") || joined.contains("电子回单号"))
                && (joined.contains("支出/存入") || joined.contains("对方账号名称") || joined.contains("用途/其他摘要"))) {
            return TEMPLATE_RCB;
        }
        if (joined.contains("[HISTORYDETAIL]") || (joined.contains("借贷标志") && joined.contains("对方单位"))) {
            return TEMPLATE_ICBC_HISTORY;
        }
        if (joined.contains("Transaction Type") || joined.contains("交易类型[")
                || (joined.contains("往账") && joined.contains("收款人名称"))) {
            return TEMPLATE_BOC;
        }
        return TEMPLATE_CCB;
    }

    /**
     * 渠道配置与文件内容冲突时：文件明确识别出具体模板则优先按文件；
     * 文件仅回落到建行默认模板时，保留渠道配置（便于同账户多格式时人工指定）。
     */
    static String resolveTemplate(String configured, String detected, KingdeeBankChannel channel) {
        if (!StringUtils.hasText(configured)) {
            return detected;
        }
        if (configured.equals(detected)) {
            return configured;
        }
        if (TEMPLATE_CCB.equals(detected) && !TEMPLATE_CCB.equals(configured)) {
            log.info("文件未明确识别银行模板，使用渠道配置 template={} channel={}",
                    configured, channel != null ? channel.getChannelKey() : null);
            return configured;
        }
        log.warn("渠道模板 {} 与文件识别 {} 不一致，按文件解析 channel={}",
                configured, detected, channel != null ? channel.getChannelKey() : null);
        return detected;
    }

    private static String joinFirstRows(List<Map<Integer, String>> raw, int maxRows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(maxRows, raw.size()); i++) {
            Map<Integer, String> row = raw.get(i);
            if (row == null) {
                continue;
            }
            for (String v : row.values()) {
                if (StringUtils.hasText(v)) {
                    sb.append(normalizeHeader(v));
                }
            }
        }
        return sb.toString();
    }

    private static List<KingdeeBankVoucherDetailExcel> readCcb(List<Map<Integer, String>> raw,
                                                               KingdeeBankChannel channel) {
        int headerIdx = findHeaderRow(raw, h ->
                h.contains("交易时间") && (h.contains("借方") || h.contains("贷方")));
        if (headerIdx < 0) {
            headerIdx = 0;
        }
        Map<Integer, String> headerRow = raw.get(headerIdx);
        ColMap cols = ColMap.fromCcbHeader(headerRow);
        String metaAccountNo = extractMetaValue(raw, headerIdx, "账号", "帐户");
        String metaAccountName = extractMetaValue(raw, headerIdx, "账户名称", "帐户名称");
        if (channel != null) {
            if (!StringUtils.hasText(metaAccountNo)) {
                metaAccountNo = channel.getAccountNo();
            }
            if (!StringUtils.hasText(metaAccountName)) {
                metaAccountName = channel.getAccountName();
            }
        }

        List<KingdeeBankVoucherDetailExcel> rows = new ArrayList<>();
        for (int i = headerIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            KingdeeBankVoucherDetailExcel row = new KingdeeBankVoucherDetailExcel();
            row.setTradeTime(cols.get(line, cols.tradeTime));
            row.setDebitAmount(cols.get(line, cols.debit));
            row.setCreditAmount(cols.get(line, cols.credit));
            row.setBalance(cols.get(line, cols.balance));
            row.setCurrency(cols.get(line, cols.currency));
            row.setCounterpartyName(cols.get(line, cols.counterpartyName));
            row.setCounterpartyAccount(cols.get(line, cols.counterpartyAccount));
            row.setCounterpartyBank(cols.get(line, cols.counterpartyBank));
            row.setBookkeepingDate(cols.get(line, cols.bookkeepingDate));
            row.setSummary(cols.get(line, cols.summary));
            row.setRemark(cols.get(line, cols.remark));
            row.setTradeSerialNo(cols.get(line, cols.tradeSerialNo));
            row.setEnterpriseSerialNo(cols.get(line, cols.enterpriseSerialNo));
            row.setVoucherKind(cols.get(line, cols.voucherKind));
            row.setBankVoucherNo(cols.get(line, cols.bankVoucherNo));
            row.setAccountNo(firstNonBlank(cols.get(line, cols.accountNo), metaAccountNo));
            row.setAccountName(firstNonBlank(cols.get(line, cols.accountName), metaAccountName));
            rows.add(row);
        }
        return rows;
    }

    private static List<KingdeeBankVoucherDetailExcel> readIcbcReceipt(List<Map<Integer, String>> raw,
                                                                       KingdeeBankChannel channel) {
        int headerIdx = findHeaderRow(raw, h -> h.contains("电子回单号码") || h.contains("付方账号"));
        if (headerIdx < 0) {
            headerIdx = 0;
        }
        Map<Integer, String> headerRow = raw.get(headerIdx);
        ColMap cols = ColMap.fromIcbcReceiptHeader(headerRow);
        String ownAccount = channel != null ? trimToNull(channel.getAccountNo()) : null;
        String ownName = channel != null ? trimToNull(channel.getAccountName()) : null;

        List<KingdeeBankVoucherDetailExcel> rows = new ArrayList<>();
        for (int i = headerIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String payerAccount = cols.get(line, cols.payerAccount);
            String payerName = cols.get(line, cols.payerName);
            String payerBank = cols.get(line, cols.payerBank);
            String payeeAccount = cols.get(line, cols.payeeAccount);
            String payeeName = cols.get(line, cols.payeeName);
            String payeeBank = cols.get(line, cols.payeeBank);
            String amountRaw = cols.get(line, cols.amount);
            BigDecimal amount = parseMoney(amountRaw);
            if (amount == null && !StringUtils.hasText(cols.get(line, cols.tradeSerialNo))) {
                continue;
            }

            boolean ownIsPayer = isOwnSide(ownAccount, ownName, payerAccount, payerName);
            boolean ownIsPayee = isOwnSide(ownAccount, ownName, payeeAccount, payeeName);
            // 默认：本方在付方为支出；否则若本方在收方为收入；再否则默认付方为本方（回单多为本方付款）
            boolean outflow;
            if (ownIsPayer && !ownIsPayee) {
                outflow = true;
            } else if (ownIsPayee && !ownIsPayer) {
                outflow = false;
            } else if (ownAccount != null && StringUtils.hasText(payerAccount)
                    && normalizeAccount(payerAccount).contains(normalizeAccount(ownAccount))) {
                outflow = true;
            } else if (ownAccount != null && StringUtils.hasText(payeeAccount)
                    && normalizeAccount(payeeAccount).contains(normalizeAccount(ownAccount))) {
                outflow = false;
            } else {
                outflow = true;
            }

            KingdeeBankVoucherDetailExcel row = new KingdeeBankVoucherDetailExcel();
            row.setReceiptNo(cols.get(line, cols.receiptNo));
            row.setAccountNo(firstNonBlank(ownAccount, outflow ? payerAccount : payeeAccount));
            row.setAccountName(firstNonBlank(ownName, outflow ? payerName : payeeName));
            row.setTradeTime(cols.get(line, cols.timestamp));
            row.setBookkeepingDate(cols.get(line, cols.bookkeepingDate));
            row.setSummary(cols.get(line, cols.summary));
            row.setPurpose(cols.get(line, cols.purpose));
            row.setRemark(cols.get(line, cols.remark));
            row.setTradeSerialNo(firstNonBlank(
                    cols.get(line, cols.tradeSerialNo),
                    cols.get(line, cols.receiptNo)));
            row.setCurrency(cols.get(line, cols.currency));
            row.setRawAmount(amount == null ? amountRaw : amount.toPlainString());
            row.setDirectionFlag(outflow ? "借" : "贷");
            if (amount != null) {
                if (outflow) {
                    row.setDebitAmount(amount.toPlainString());
                    row.setCreditAmount("0");
                } else {
                    row.setDebitAmount("0");
                    row.setCreditAmount(amount.toPlainString());
                }
            }
            if (outflow) {
                row.setCounterpartyName(payeeName);
                row.setCounterpartyAccount(payeeAccount);
                row.setCounterpartyBank(payeeBank);
            } else {
                row.setCounterpartyName(payerName);
                row.setCounterpartyAccount(payerAccount);
                row.setCounterpartyBank(payerBank);
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<KingdeeBankVoucherDetailExcel> readIcbcHistory(List<Map<Integer, String>> raw,
                                                                       KingdeeBankChannel channel) {
        int headerIdx = findHeaderRow(raw, h -> h.contains("借贷标志") || h.contains("对方单位"));
        if (headerIdx < 0) {
            headerIdx = raw.size() > 1 ? 1 : 0;
        }
        Map<Integer, String> headerRow = raw.get(headerIdx);
        ColMap cols = ColMap.fromIcbcHistoryHeader(headerRow);

        List<KingdeeBankVoucherDetailExcel> rows = new ArrayList<>();
        for (int i = headerIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String direction = cols.get(line, cols.directionFlag);
            String remark = joinNonBlank(" ",
                    cols.get(line, cols.postscript),
                    cols.get(line, cols.extraInfo));
            String amountRaw = cols.get(line, cols.amount);
            BigDecimal amount = parseMoney(amountRaw);
            if (amount == null) {
                amount = parseMoneyFromText(remark);
            }
            if (amount == null) {
                amount = parseMoneyFromText(cols.get(line, cols.summary));
            }

            boolean outflow = direction != null && (direction.contains("借") || "D".equalsIgnoreCase(direction));
            boolean inflow = direction != null && (direction.contains("贷") || "C".equalsIgnoreCase(direction));
            if (!outflow && !inflow) {
                outflow = true;
            } else if (inflow) {
                outflow = false;
            }

            KingdeeBankVoucherDetailExcel row = new KingdeeBankVoucherDetailExcel();
            if (channel != null) {
                row.setAccountNo(channel.getAccountNo());
                row.setAccountName(channel.getAccountName());
            }
            row.setTradeTime(cols.get(line, cols.tradeTime));
            row.setBookkeepingDate(cols.get(line, cols.tradeTime));
            row.setDirectionFlag(direction);
            row.setCounterpartyName(cols.get(line, cols.counterpartyName));
            row.setCounterpartyAccount(cols.get(line, cols.counterpartyAccount));
            row.setCounterpartyBank(cols.get(line, cols.counterpartyBank));
            row.setPurpose(cols.get(line, cols.purpose));
            row.setSummary(cols.get(line, cols.summary));
            row.setRemark(remark);
            row.setBankVoucherNo(cols.get(line, cols.bankVoucherNo));
            // 禁止用摘要顶替流水号：同摘要多笔（如多笔手续费）会互相覆盖
            String serial = firstNonBlank(
                    cols.get(line, cols.tradeSerialNo),
                    cols.get(line, cols.bankVoucherNo));
            row.setTradeSerialNo(serial);
            if (amount != null) {
                row.setRawAmount(amount.toPlainString());
                if (outflow) {
                    row.setDebitAmount(amount.toPlainString());
                    row.setCreditAmount("0");
                } else {
                    row.setDebitAmount("0");
                    row.setCreditAmount(amount.toPlainString());
                }
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<KingdeeBankVoucherDetailExcel> readBoc(List<Map<Integer, String>> raw,
                                                              KingdeeBankChannel channel) {
        int headerIdx = findHeaderRow(raw, h ->
                h.contains("交易类型") || h.contains("Transaction Type") || h.contains("交易金额"));
        if (headerIdx < 0) {
            headerIdx = 0;
        }
        Map<Integer, String> headerRow = raw.get(headerIdx);
        ColMap cols = ColMap.fromBocHeader(headerRow);
        String metaAccount = extractMetaValue(raw, headerIdx, "查询账号", "Inquirer account");
        if (!StringUtils.hasText(metaAccount) && channel != null) {
            metaAccount = channel.getAccountNo();
        }
        String ownAccount = firstNonBlank(metaAccount, channel != null ? channel.getAccountNo() : null);
        String ownName = channel != null ? channel.getAccountName() : null;

        List<KingdeeBankVoucherDetailExcel> rows = new ArrayList<>();
        for (int i = headerIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String txType = cols.get(line, cols.txType);
            String bizType = cols.get(line, cols.extraInfo);
            String amountRaw = cols.get(line, cols.amount);
            BigDecimal signed = parseSignedAmount(amountRaw);
            if (signed == null && !StringUtils.hasText(cols.get(line, cols.tradeSerialNo))) {
                continue;
            }

            boolean outflow;
            if (txType != null && txType.contains("往账")) {
                outflow = true;
            } else if (txType != null && txType.contains("来账")) {
                outflow = false;
            } else if (signed != null) {
                outflow = signed.compareTo(BigDecimal.ZERO) < 0;
            } else {
                outflow = true;
            }
            BigDecimal amount = signed == null ? null : signed.abs();

            String payerAccount = cols.get(line, cols.payerAccount);
            String payerName = cols.get(line, cols.payerName);
            String payerBank = cols.get(line, cols.payerBank);
            String payeeAccount = cols.get(line, cols.payeeAccount);
            String payeeName = cols.get(line, cols.payeeName);
            String payeeBank = cols.get(line, cols.payeeBank);

            KingdeeBankVoucherDetailExcel row = new KingdeeBankVoucherDetailExcel();
            row.setAccountNo(firstNonBlank(ownAccount, outflow ? payerAccount : payeeAccount));
            row.setAccountName(firstNonBlank(ownName, outflow ? payerName : payeeName));
            String date = cols.get(line, cols.tradeDate);
            String time = cols.get(line, cols.tradeTime);
            row.setTradeTime(joinNonBlank(" ", date, time));
            row.setBookkeepingDate(firstNonBlank(date, cols.get(line, cols.valueDate)));
            row.setCurrency(cols.get(line, cols.currency));
            row.setBalance(cols.get(line, cols.balance));
            row.setSummary(cols.get(line, cols.summary));
            row.setPurpose(firstNonBlank(cols.get(line, cols.purpose), bizType));
            row.setRemark(joinNonBlank(" ",
                    cols.get(line, cols.postscript),
                    cols.get(line, cols.remark),
                    // 业务类型写入备注末尾，便于实时缴税/代发划转等本地识别
                    StringUtils.hasText(bizType) && !bizType.equals(row.getPurpose()) ? bizType : null));
            row.setTradeSerialNo(firstNonBlank(
                    cols.get(line, cols.tradeSerialNo),
                    cols.get(line, cols.recordId),
                    cols.get(line, cols.bankVoucherNo)));
            row.setBankVoucherNo(cols.get(line, cols.bankVoucherNo));
            row.setVoucherKind(firstNonBlank(cols.get(line, cols.voucherKind), bizType));
            row.setReceiptNo(cols.get(line, cols.recordId));
            row.setDirectionFlag(txType);
            if (amount != null) {
                row.setRawAmount(amount.toPlainString());
                if (outflow) {
                    row.setDebitAmount(amount.toPlainString());
                    row.setCreditAmount("0");
                } else {
                    row.setDebitAmount("0");
                    row.setCreditAmount(amount.toPlainString());
                }
            }
            if (outflow) {
                row.setCounterpartyName(payeeName);
                row.setCounterpartyAccount(payeeAccount);
                row.setCounterpartyBank(payeeBank);
            } else {
                row.setCounterpartyName(payerName);
                row.setCounterpartyAccount(payerAccount);
                row.setCounterpartyBank(payerBank);
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * 农商行客户电子回单：账号/账户名称在表头元数据；明细含支出/存入、对方账号名称、摘要。
     */
    private static List<KingdeeBankVoucherDetailExcel> readRcb(List<Map<Integer, String>> raw,
                                                              KingdeeBankChannel channel) {
        int headerIdx = findHeaderRow(raw, h ->
                h.contains("电子回单号") || (h.contains("支出") && h.contains("存入"))
                        || h.contains("对方账号名称"));
        if (headerIdx < 0) {
            headerIdx = 0;
        }
        Map<Integer, String> headerRow = raw.get(headerIdx);
        ColMap cols = ColMap.fromRcbHeader(headerRow);
        String metaAccount = extractMetaValue(raw, headerIdx, "账号", "帐户");
        String metaAccountName = extractMetaValue(raw, headerIdx, "账户名称", "帐户名称");
        if (channel != null) {
            if (!StringUtils.hasText(metaAccount)) {
                metaAccount = channel.getAccountNo();
            }
            if (!StringUtils.hasText(metaAccountName)) {
                metaAccountName = channel.getAccountName();
            }
        }

        List<KingdeeBankVoucherDetailExcel> rows = new ArrayList<>();
        for (int i = headerIdx + 1; i < raw.size(); i++) {
            Map<Integer, String> line = raw.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String serial = firstNonBlank(
                    cols.get(line, cols.tradeSerialNo),
                    cols.get(line, cols.receiptNo));
            String amountRaw = cols.get(line, cols.amount);
            BigDecimal signed = parseSignedAmount(amountRaw);
            String direction = cols.get(line, cols.directionFlag);
            if (signed == null && !StringUtils.hasText(serial)) {
                continue;
            }

            boolean outflow;
            if (direction != null && direction.contains("支出")) {
                outflow = true;
            } else if (direction != null && (direction.contains("存入") || direction.contains("收入"))) {
                outflow = false;
            } else if (signed != null) {
                outflow = signed.compareTo(BigDecimal.ZERO) < 0;
            } else {
                continue;
            }
            BigDecimal amount = signed == null ? null : signed.abs();

            KingdeeBankVoucherDetailExcel row = new KingdeeBankVoucherDetailExcel();
            row.setAccountNo(metaAccount);
            row.setAccountName(metaAccountName);
            String tradeTime = cols.get(line, cols.tradeTime);
            row.setTradeTime(tradeTime);
            row.setBookkeepingDate(normalizeChineseDate(tradeTime));
            row.setBalance(cols.get(line, cols.balance));
            row.setSummary(cols.get(line, cols.summary));
            row.setPurpose(cols.get(line, cols.purpose));
            row.setRemark(cols.get(line, cols.remark));
            row.setTradeSerialNo(serial);
            row.setReceiptNo(cols.get(line, cols.receiptNo));
            row.setDirectionFlag(direction);
            row.setCounterpartyName(cols.get(line, cols.counterpartyName));
            row.setCounterpartyAccount(cols.get(line, cols.counterpartyAccount));
            row.setCounterpartyBank(cols.get(line, cols.counterpartyBank));
            if (amount != null) {
                row.setRawAmount(amount.toPlainString());
                if (outflow) {
                    row.setDebitAmount(amount.toPlainString());
                    row.setCreditAmount("0");
                } else {
                    row.setDebitAmount("0");
                    row.setCreditAmount(amount.toPlainString());
                }
            }
            rows.add(row);
        }
        return rows;
    }

    /** 将「2026年08月03日」等转为 yyyyMMdd，便于记账日期匹配 */
    private static String normalizeChineseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 8);
        }
        return raw.trim();
    }

    private static int findHeaderRow(List<Map<Integer, String>> raw, Predicate<String> matcher) {
        int best = -1;
        int bestScore = -1;
        for (int i = 0; i < Math.min(20, raw.size()); i++) {
            Map<Integer, String> row = raw.get(i);
            if (row == null) {
                continue;
            }
            StringBuilder joined = new StringBuilder();
            int nonempty = 0;
            for (String v : row.values()) {
                if (StringUtils.hasText(v)) {
                    nonempty++;
                    joined.append(normalizeHeader(v));
                }
            }
            String text = joined.toString();
            if (matcher.test(text) && nonempty > bestScore) {
                bestScore = nonempty;
                best = i;
            }
        }
        return best;
    }

    private static String extractMetaValue(List<Map<Integer, String>> raw, int beforeRow,
                                           String... labels) {
        for (int i = 0; i < beforeRow && i < raw.size(); i++) {
            Map<Integer, String> row = raw.get(i);
            if (row == null) {
                continue;
            }
            List<Map.Entry<Integer, String>> cells = new ArrayList<>(row.entrySet());
            cells.sort(Map.Entry.comparingByKey());
            for (int c = 0; c < cells.size(); c++) {
                String label = normalizeHeader(cells.get(c).getValue());
                for (String want : labels) {
                    if (label.contains(normalizeHeader(want)) && c + 1 < cells.size()) {
                        String val = trimToNull(cells.get(c + 1).getValue());
                        if (val != null) {
                            return val;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isOwnSide(String ownAccount, String ownName,
                                     String sideAccount, String sideName) {
        if (StringUtils.hasText(ownAccount) && StringUtils.hasText(sideAccount)) {
            String a = normalizeAccount(ownAccount);
            String b = normalizeAccount(sideAccount);
            if (a.equals(b) || b.contains(a) || a.contains(b.replace("*", ""))) {
                return true;
            }
        }
        if (StringUtils.hasText(ownName) && StringUtils.hasText(sideName)) {
            return ownName.trim().equals(sideName.trim());
        }
        return false;
    }

    private static String normalizeAccount(String account) {
        if (account == null) {
            return "";
        }
        return account.replace(" ", "").replace("*", "").trim();
    }

    private static BigDecimal parseMoney(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        Matcher m = YUAN_AMOUNT.matcher(raw.replace(" ", ""));
        if (m.find()) {
            return toAmount(m.group(1));
        }
        String cleaned = raw.trim()
                .replace(",", "")
                .replace("，", "")
                .replace(" ", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace("元", "");
        if (cleaned.isEmpty() || "-".equals(cleaned)) {
            return null;
        }
        Matcher digits = Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)").matcher(cleaned);
        if (digits.find()) {
            return toAmount(digits.group(1));
        }
        return null;
    }

    private static BigDecimal parseMoneyFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher m = PLAIN_AMOUNT.matcher(text);
        if (m.find()) {
            return toAmount(m.group(1));
        }
        return parseMoney(text);
    }

    private static BigDecimal parseSignedAmount(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String cleaned = raw.trim()
                .replace(",", "")
                .replace("，", "")
                .replace(" ", "");
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return parseMoney(raw);
        }
    }

    private static BigDecimal toAmount(String raw) {
        try {
            return new BigDecimal(raw.replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static String joinNonBlank(String sep, String... values) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (String v : values) {
                if (!StringUtils.hasText(v)) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(v.trim());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String t = value.replace("\u00A0", " ").trim();
        return t.isEmpty() ? null : t;
    }

    static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .replace("（", "(")
                .replace("）", ")")
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\t", "")
                .replace("　", "");
    }

    private static final class ColMap {
        Integer tradeTime;
        Integer debit;
        Integer credit;
        Integer balance;
        Integer currency;
        Integer counterpartyName;
        Integer counterpartyAccount;
        Integer counterpartyBank;
        Integer bookkeepingDate;
        Integer summary;
        Integer remark;
        Integer tradeSerialNo;
        Integer enterpriseSerialNo;
        Integer voucherKind;
        Integer bankVoucherNo;
        Integer accountNo;
        Integer accountName;
        Integer receiptNo;
        Integer payerAccount;
        Integer payerName;
        Integer payerBank;
        Integer payeeAccount;
        Integer payeeName;
        Integer payeeBank;
        Integer amount;
        Integer purpose;
        Integer timestamp;
        Integer directionFlag;
        Integer postscript;
        Integer extraInfo;
        Integer txType;
        Integer tradeDate;
        Integer valueDate;
        Integer recordId;

        static ColMap fromCcbHeader(Map<Integer, String> headerRow) {
            Map<Integer, String> normalized = normalizeMap(headerRow);
            ColMap m = new ColMap();
            m.tradeTime = find(normalized, h -> h.contains("交易时间"));
            m.debit = find(normalized, h -> h.contains("借方") && (h.contains("发生额") || h.contains("支取") || h.contains("支出")));
            m.credit = find(normalized, h -> h.contains("贷方") && (h.contains("发生额") || h.contains("收入")));
            m.balance = find(normalized, h -> h.equals("余额") || h.startsWith("余额"));
            m.currency = find(normalized, h -> h.contains("币种"));
            m.counterpartyName = find(normalized, h -> h.contains("对方户名"));
            m.counterpartyAccount = find(normalized, h -> h.contains("对方账号") || h.contains("对方帐户"));
            m.counterpartyBank = find(normalized, h -> h.contains("对方开户"));
            m.bookkeepingDate = find(normalized, h -> h.contains("记账日期") || h.contains("记帐日期"));
            m.summary = find(normalized, h -> h.equals("摘要") || h.startsWith("摘要"));
            m.remark = find(normalized, h -> h.equals("备注") || h.startsWith("备注"));
            m.tradeSerialNo = find(normalized, h ->
                    h.contains("交易流水号") || h.contains("账户明细编号") || h.contains("账户序号"));
            m.enterpriseSerialNo = find(normalized, h -> h.contains("企业流水号"));
            m.voucherKind = find(normalized, h -> h.contains("凭证种类"));
            m.bankVoucherNo = find(normalized, h -> h.equals("凭证号") || (h.contains("凭证号") && !h.contains("金蝶")));
            m.accountNo = find(normalized, h -> h.equals("账号") || h.equals("帐户"));
            m.accountName = find(normalized, h -> h.contains("账户名称") || h.contains("帐户名称"));
            return m;
        }

        static ColMap fromIcbcReceiptHeader(Map<Integer, String> headerRow) {
            Map<Integer, String> normalized = normalizeMap(headerRow);
            ColMap m = new ColMap();
            m.receiptNo = find(normalized, h -> h.contains("电子回单"));
            m.payerAccount = find(normalized, h -> h.contains("付方账号") || h.contains("付方帐户"));
            m.payerName = find(normalized, h -> h.contains("付方账户名称") || h.contains("付方帐户名称"));
            m.payerBank = find(normalized, h -> h.contains("付方开户"));
            m.payeeAccount = find(normalized, h -> h.contains("收方账号") || h.contains("收方帐户"));
            m.payeeName = find(normalized, h -> h.contains("收方账户名称") || h.contains("收方帐户名称"));
            m.payeeBank = find(normalized, h -> h.contains("收方开户"));
            m.amount = find(normalized, h -> h.equals("金额") || h.startsWith("金额"));
            m.currency = find(normalized, h -> h.contains("币种"));
            m.summary = find(normalized, h -> h.equals("摘要") || h.startsWith("摘要"));
            m.purpose = find(normalized, h -> h.equals("用途") || h.startsWith("用途"));
            m.tradeSerialNo = find(normalized, h -> h.contains("交易流水号"));
            m.timestamp = find(normalized, h -> h.contains("时间戳"));
            m.remark = find(normalized, h -> h.equals("备注") || h.startsWith("备注"));
            m.bookkeepingDate = find(normalized, h -> h.contains("记账日期") || h.contains("记帐日期"));
            return m;
        }

        static ColMap fromIcbcHistoryHeader(Map<Integer, String> headerRow) {
            Map<Integer, String> normalized = normalizeMap(headerRow);
            ColMap m = new ColMap();
            m.bankVoucherNo = find(normalized, h -> h.contains("凭证号"));
            m.counterpartyAccount = find(normalized, h -> h.contains("对方账号") || h.contains("对方帐户"));
            m.tradeTime = find(normalized, h -> h.contains("交易时间"));
            m.directionFlag = find(normalized, h -> h.contains("借贷标志") || h.contains("借贷"));
            m.counterpartyName = find(normalized, h -> h.contains("对方单位") || h.contains("对方户名"));
            m.counterpartyBank = find(normalized, h -> h.contains("对方行号") || h.contains("对方开户"));
            m.purpose = find(normalized, h -> h.contains("用途"));
            m.summary = find(normalized, h -> h.equals("摘要") || h.startsWith("摘要"));
            m.postscript = find(normalized, h -> h.contains("附言"));
            m.extraInfo = find(normalized, h -> h.contains("回单个性化") || h.contains("个性化信息"));
            m.amount = find(normalized, h -> h.contains("发生额") || h.equals("金额") || h.startsWith("金额")
                    || h.contains("交易金额"));
            m.tradeSerialNo = find(normalized, h -> h.contains("交易流水号") || h.contains("流水号"));
            return m;
        }

        static ColMap fromBocHeader(Map<Integer, String> headerRow) {
            Map<Integer, String> normalized = normalizeMap(headerRow);
            ColMap m = new ColMap();
            m.txType = find(normalized, h -> h.contains("交易类型") || h.contains("TransactionType"));
            m.extraInfo = find(normalized, h -> h.contains("业务类型") || h.contains("Businesstype"));
            m.payerBank = find(normalized, h -> h.contains("付款人开户行名") || h.contains("Payeraccountbank"));
            m.payerAccount = find(normalized, h -> h.contains("付款人账号") || h.contains("DebitAccountNo"));
            m.payerName = find(normalized, h -> h.contains("付款人名称") || h.contains("Payer'sName") || h.contains("PayersName"));
            m.payeeBank = find(normalized, h -> h.contains("收款人开户行名") || h.contains("Beneficiaryaccountbank"));
            m.payeeAccount = find(normalized, h -> h.contains("收款人账号") || h.contains("Payee'sAccount") || h.contains("PayeesAccount"));
            m.payeeName = find(normalized, h -> h.contains("收款人名称") || h.contains("Payee'sName") || h.contains("PayeesName"));
            m.tradeDate = find(normalized, h -> h.contains("交易日期") || h.contains("TransactionDate"));
            m.tradeTime = find(normalized, h -> h.contains("交易时间") || h.contains("Transactiontime"));
            m.currency = find(normalized, h -> h.contains("交易货币") || h.contains("TradeCurrency"));
            m.amount = find(normalized, h -> h.contains("交易金额") || h.contains("TradeAmount"));
            m.balance = find(normalized, h -> h.contains("交易后余额") || h.contains("After-transaction"));
            m.valueDate = find(normalized, h -> h.contains("起息日期") || h.contains("ValueDate"));
            m.tradeSerialNo = find(normalized, h -> h.contains("交易流水号") || h.contains("Transactionreferencenumber"));
            m.voucherKind = find(normalized, h -> h.contains("凭证类型") || h.contains("Vouchertype"));
            m.bankVoucherNo = find(normalized, h -> h.contains("凭证号码") || h.contains("Vouchernumber"));
            m.recordId = find(normalized, h -> h.contains("记录标识号") || h.contains("RecordID"));
            m.summary = find(normalized, h -> h.startsWith("摘要") || h.contains("Reference]"));
            m.purpose = find(normalized, h -> h.contains("用途") || h.contains("Purpose"));
            m.postscript = find(normalized, h -> h.contains("交易附言") || h.contains("Remark]"));
            m.remark = find(normalized, h -> h.startsWith("备注") || h.contains("Remarks"));
            return m;
        }

        static ColMap fromRcbHeader(Map<Integer, String> headerRow) {
            Map<Integer, String> normalized = normalizeMap(headerRow);
            ColMap m = new ColMap();
            m.receiptNo = find(normalized, h -> h.contains("电子回单号"));
            m.tradeTime = find(normalized, h -> h.contains("交易时间"));
            m.counterpartyAccount = find(normalized, h -> h.equals("对方账号") || h.equals("对方帐户"));
            m.counterpartyName = find(normalized, h -> h.contains("对方账号名称") || h.contains("对方帐户名称")
                    || h.contains("对方户名"));
            m.directionFlag = find(normalized, h -> h.contains("支出/存入") || h.equals("借贷标志"));
            m.amount = find(normalized, h -> h.contains("交易金额") || h.equals("金额") || h.startsWith("金额"));
            m.balance = find(normalized, h -> h.contains("账户余额") || h.equals("余额") || h.startsWith("余额"));
            m.summary = find(normalized, h -> h.equals("摘要") || h.startsWith("摘要"));
            m.purpose = find(normalized, h -> h.contains("用途") || h.contains("其他摘要"));
            m.counterpartyBank = find(normalized, h -> h.contains("机构名称") || h.contains("营业机构"));
            m.tradeSerialNo = find(normalized, h -> h.contains("交易流水"));
            return m;
        }

        private static Map<Integer, String> normalizeMap(Map<Integer, String> headerRow) {
            Map<Integer, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> e : headerRow.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                normalized.put(e.getKey(), normalizeHeader(e.getValue()));
            }
            return normalized;
        }

        private static Integer find(Map<Integer, String> headers, Predicate<String> matcher) {
            for (Map.Entry<Integer, String> e : headers.entrySet()) {
                String h = e.getValue();
                if (StringUtils.hasText(h) && matcher.test(h)) {
                    return e.getKey();
                }
            }
            return null;
        }

        String get(Map<Integer, String> line, Integer col) {
            if (col == null || line == null) {
                return null;
            }
            String v = line.get(col);
            if (!StringUtils.hasText(v)) {
                return null;
            }
            // 农商行等导出常带 NBSP
            return v.replace("\u00A0", " ").trim();
        }
    }
}
