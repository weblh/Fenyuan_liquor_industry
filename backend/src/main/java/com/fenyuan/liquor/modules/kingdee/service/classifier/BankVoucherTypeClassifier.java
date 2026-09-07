package com.fenyuan.liquor.modules.kingdee.service.classifier;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 根据建行明细摘要/备注/借贷方向识别凭证业务类型（对照金蝶凭证截图）。
 * <p>
 * 仅 {@link #isWritable(String)} 为 true 的类型会自动写入金蝶；
 * 社保/工资/贴现/其他等需人工拆分科目的类型只识别、不写入。
 */
public final class BankVoucherTypeClassifier {

    public static final String FEE = "FEE";
    public static final String FINANCE_FEE = "FINANCE_FEE";
    public static final String GOODS = "GOODS";
    public static final String FREIGHT = "FREIGHT";
    public static final String EXPENSE = "EXPENSE";
    public static final String TAX = "TAX";
    public static final String PERSONAL_TAX = "PERSONAL_TAX";
    public static final String SOCIAL = "SOCIAL";
    public static final String SALARY = "SALARY";
    public static final String RECEIPT = "RECEIPT";
    public static final String DISCOUNT_BA = "DISCOUNT_BA";
    public static final String DISCOUNT_FIN = "DISCOUNT_FIN";
    public static final String INTEREST = "INTEREST";
    public static final String LOAN = "LOAN";
    public static final String OTHER = "OTHER";

    private static final Map<String, String> TYPE_NAMES = new LinkedHashMap<>();

    /** 可自动生成借贷科目并写入金蝶的类型 */
    private static final Set<String> WRITABLE_TYPES = new LinkedHashSet<>();

    static {
        TYPE_NAMES.put(FEE, "支付手续费");
        TYPE_NAMES.put(FINANCE_FEE, "支付融资手续费");
        TYPE_NAMES.put(GOODS, "支付货款");
        TYPE_NAMES.put(FREIGHT, "支付运费");
        TYPE_NAMES.put(EXPENSE, "支付费用");
        TYPE_NAMES.put(TAX, "缴纳税费");
        TYPE_NAMES.put(PERSONAL_TAX, "缴纳个税");
        TYPE_NAMES.put(SOCIAL, "缴纳社保");
        TYPE_NAMES.put(SALARY, "支付工资");
        TYPE_NAMES.put(RECEIPT, "收到货款");
        TYPE_NAMES.put(DISCOUNT_BA, "银承贴现回款");
        TYPE_NAMES.put(DISCOUNT_FIN, "融单贴现到账");
        TYPE_NAMES.put(INTEREST, "收到银行利息");
        TYPE_NAMES.put(LOAN, "贷款相关");
        TYPE_NAMES.put(OTHER, "其他");

        WRITABLE_TYPES.add(FEE);
        WRITABLE_TYPES.add(FINANCE_FEE);
        WRITABLE_TYPES.add(GOODS);
        WRITABLE_TYPES.add(FREIGHT);
        WRITABLE_TYPES.add(EXPENSE);
        WRITABLE_TYPES.add(TAX);
        WRITABLE_TYPES.add(PERSONAL_TAX);
        WRITABLE_TYPES.add(RECEIPT);
        WRITABLE_TYPES.add(INTEREST);
        WRITABLE_TYPES.add(LOAN);
    }

    private BankVoucherTypeClassifier() {
    }

    public static String typeName(String type) {
        return TYPE_NAMES.getOrDefault(type, TYPE_NAMES.get(OTHER));
    }

    public static Map<String, String> allTypes() {
        return new LinkedHashMap<>(TYPE_NAMES);
    }

    public static Set<String> writableTypes() {
        return Collections.unmodifiableSet(WRITABLE_TYPES);
    }

    public static boolean isWritable(String type) {
        return type != null && WRITABLE_TYPES.contains(type.trim());
    }

    /** 不可写入时的说明（可写入返回 null） */
    public static String nonWritableReason(String type) {
        if (isWritable(type)) {
            return null;
        }
        if (SOCIAL.equals(type) || SALARY.equals(type)
                || DISCOUNT_BA.equals(type) || DISCOUNT_FIN.equals(type)) {
            return typeName(type) + "需拆分多科目，仅识别不写入金蝶";
        }
        if (OTHER.equals(type)) {
            return "理财/内部转账等无法自动匹配科目，仅识别不写入金蝶";
        }
        return typeName(type) + "无法自动匹配科目内容，仅识别不写入金蝶";
    }

    /**
     * 银行流水本地明确为「付款手续费」独立流水（借方支出，摘要「手续费/收费/FEE」）。
     * 扫码收款摘要里嵌套的 FEE / 通联 SQEEFF 不归此类（由扫码收款拆分手续费分录处理）。
     */
    public static boolean isLocalFeeSignal(String summary, String remark) {
        if (isQrReceiptSignal(summary, remark, null)) {
            return false;
        }
        String s = nullToEmpty(summary);
        String r = nullToEmpty(remark);
        return s.contains("手续费") || s.contains("收费") || r.contains("手续费")
                || "FEE".equalsIgnoreCase(s.trim())
                || s.toUpperCase().startsWith("FEE");
    }

    /**
     * 银行「汇款/跨行转账」手续费独立流水（与货款分行、各自一张银字）。
     * 例：摘要「对公跨行转账汇款手续费」。扫码摘要内嵌 FEE 不归此类。
     */
    public static boolean isBankRemittanceFee(String summary, String remark) {
        if (isQrReceiptSignal(summary, remark, null)) {
            return false;
        }
        String text = nullToEmpty(summary) + " " + nullToEmpty(remark);
        return text.contains("汇款手续费")
                || text.contains("转账汇款手续费")
                || text.contains("网银手续费")
                || text.contains("电子汇划费")
                || (text.contains("跨行转账") && text.contains("手续费"))
                || (text.contains("对公跨行") && text.contains("手续费"));
    }

    /**
     * 扫码收款：用途/摘要含银联条码、商户清算、美团/钱袋宝提现等，或对方为清算户。
     */
    public static boolean isQrReceiptSignal(String summary, String remark, String purpose) {
        return isQrReceiptSignal(summary, remark, purpose, null);
    }

    public static boolean isQrReceiptSignal(String summary, String remark, String purpose,
                                            String counterpartyName) {
        String text = nullToEmpty(summary) + " " + nullToEmpty(remark) + " " + nullToEmpty(purpose);
        String upper = text.toUpperCase();
        if (text.contains("银联条码")
                || text.contains("银联二维码")
                || text.contains("二维码")
                || text.contains("条码支付")
                || text.contains("扫码")
                || text.contains("商户清算")
                || text.contains("银联清算")
                || text.contains("美团外卖")
                || text.contains("美团")
                || text.contains("钱袋宝")
                || text.contains("待结算")
                || text.contains("微信收款")
                || upper.contains("QRA")
                || upper.contains("UNIONPAY")
                || upper.contains("REB2B")
                // 通联清算附言：TL20260702SS5Z7F0.48 / TL...SQEEFF...
                || upper.contains("SQEEFF")
                || upper.matches("(?s).*TL20\\d{6}.*")) {
            return true;
        }
        return isQrSettlementCounterparty(counterpartyName);
    }

    /** 扫码清算对手：中行银行卡部、银联、通联、微信/支付宝待结算户等，非客户对公户名 */
    public static boolean isQrSettlementCounterparty(String counterpartyName) {
        String name = nullToEmpty(counterpartyName);
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return name.contains("银行卡部")
                || name.contains("银联")
                || name.contains("通联支付")
                || name.contains("钱袋宝")
                || name.contains("微信支付")
                || name.contains("微信")
                || name.contains("支付宝")
                || name.contains("财付通")
                || name.contains("待结算")
                || (name.contains("中国银行") && name.contains("清算"))
                || name.contains("收单");
    }

    /**
     * 业务规则（中行 F 列付款人等）：贷方收入且对方不是本公司 → 对公收款或扫码收款。
     */
    public static boolean isExternalReceiptPayer(String counterpartyName, String ownCompanyName) {
        String name = nullToEmpty(counterpartyName).trim();
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return !isOwnCompanyName(name, ownCompanyName);
    }

    /** 是否为本公司户名（渠道账户名 / 共成全称别名） */
    public static boolean isOwnCompanyName(String name, String ownCompanyName) {
        String n = nullToEmpty(name).trim();
        if (!StringUtils.hasText(n)) {
            return false;
        }
        String own = nullToEmpty(ownCompanyName).trim();
        if (StringUtils.hasText(own) && (n.equals(own) || n.contains(own) || own.contains(n))) {
            return true;
        }
        // 无渠道户名时，按已知本公司关键字兜底
        return n.contains("汾源酒业") || n.contains("广东汾源");
    }

    public static String localFeeType(String summary, String remark) {
        String r = nullToEmpty(remark);
        return r.contains("融资") ? FINANCE_FEE : FEE;
    }

    /**
     * 摘要/备注关键词可明确判定的类型。
     * 反推时：金蝶税费科目优先；货款/运费可被本地手续费信号覆盖。
     */
    public static String classifyHighConfidence(String summary, String remark, BigDecimal debit, BigDecimal credit) {
        return classifyHighConfidence(summary, remark, debit, credit, null, null);
    }

    public static String classifyHighConfidence(String summary, String remark, BigDecimal debit, BigDecimal credit,
                                                String counterpartyName) {
        return classifyHighConfidence(summary, remark, debit, credit, counterpartyName, null);
    }

    public static String classifyHighConfidence(String summary, String remark, BigDecimal debit, BigDecimal credit,
                                                String counterpartyName, String ownCompanyName) {
        String s = nullToEmpty(summary);
        String r = nullToEmpty(remark);
        boolean isDebit = debit != null && debit.compareTo(BigDecimal.ZERO) > 0;
        boolean isCredit = credit != null && credit.compareTo(BigDecimal.ZERO) > 0;

        if (r.contains("融资手续费")) {
            return FINANCE_FEE;
        }
        // 建行摘要「手续费/收费」且对方户名通常为空，优先认定银行手续费
        if (isLocalFeeSignal(summary, remark)) {
            return localFeeType(summary, remark);
        }
        if (r.contains("个人所得税") || (s.contains("缴税") && r.contains("个税"))) {
            return PERSONAL_TAX;
        }
        if (s.contains("社保") || r.contains("医疗保险") || r.contains("养老保险")
                || r.contains("工伤保险") || r.contains("失业保险") || r.contains("社保")
                || r.contains("公积金") || s.contains("公积金")
                || (nullToEmpty(counterpartyName).contains("公积金"))) {
            return SOCIAL;
        }
        if (s.contains("缴税") || s.contains("纳税") || s.contains("实时缴税")
                || r.contains("缴税") || r.contains("纳税") || r.contains("实时缴税")
                || r.contains("增值税") || r.contains("城市维护建设税") || r.contains("教育费附加")
                || r.contains("税务局") || nullToEmpty(counterpartyName).contains("税务")
                || nullToEmpty(counterpartyName).contains("代理支库")) {
            return TAX;
        }
        if (s.contains("结息") || (isCredit && (s.contains("利息") || r.contains("结息")))) {
            return INTEREST;
        }
        if (r.contains("融资放款") || r.contains("融单") || (isCredit && r.contains("融资款"))) {
            return DISCOUNT_FIN;
        }
        if (r.contains("票据包") || r.contains("银承") || (isCredit && r.contains("贴现"))) {
            return DISCOUNT_BA;
        }
        if ("工资".equals(r) || r.contains("工资") || s.contains("工资")
                || s.contains("代发划转") || r.contains("代发划转") || r.contains("代发")) {
            return SALARY;
        }
        // 理财申购等非经营流水 → 其他（不写入）
        if (s.contains("理财") || r.contains("理财") || r.contains("理财申购")
                || nullToEmpty(counterpartyName).contains("理财")) {
            return OTHER;
        }
        // 贷款仅限借方支出，避免贷方对公收款摘要误伤
        if (isDebit && ("贷款".equals(r) || s.contains("发放贷款") || s.contains("贷款") || r.contains("贷款"))) {
            return LOAN;
        }
        // 内部互转（对方为本公司其他银行账户）→ 其他，需人工处理
        if (isDebit && isOwnCompanyName(counterpartyName, ownCompanyName)
                && (r.contains("转账") || s.contains("转账") || "转账".equals(r.trim()))) {
            return OTHER;
        }
        if ("运费".equals(r) || "运输费".equals(r) || r.contains("运费") || s.contains("运费")
                || r.contains("快递费") || s.contains("快递")) {
            return FREIGHT;
        }
        if ("费用".equals(r) || "差旅费".equals(r) || r.contains("差旅") || r.contains("报销")
                || s.contains("报销") || r.contains("招待费") || r.contains("办公用品")) {
            return EXPENSE;
        }
        if ("货款".equals(r) || "加工费".equals(r) || r.contains("货款") || r.contains("柴油")
                || r.contains("边角料") || r.contains("采购款") || r.contains("采购")) {
            return GOODS;
        }
        // 贷方：付款人/对方不是本公司 → 对公收款或扫码收款（业务规则）
        if (isCredit && (isQrReceiptSignal(summary, remark, null, counterpartyName)
                || isCorporateTransferReceiptSignal(summary, remark, null)
                || isExternalReceiptPayer(counterpartyName, ownCompanyName)
                || looksLikeCompanyCounterparty(counterpartyName)
                || s.contains("电子汇入") || s.contains("退汇") || s.contains("代理付款")
                || s.contains("汇入") || r.contains("货款") || r.contains("租金")
                || r.contains("回款") || r.contains("收款"))) {
            return RECEIPT;
        }
        return null;
    }

    public static String classify(String summary, String remark, BigDecimal debit, BigDecimal credit) {
        return classify(summary, remark, debit, credit, null, null);
    }

    public static String classify(String summary, String remark, BigDecimal debit, BigDecimal credit,
                                  String counterpartyName) {
        return classify(summary, remark, debit, credit, counterpartyName, null);
    }

    public static String classify(String summary, String remark, BigDecimal debit, BigDecimal credit,
                                  String counterpartyName, String ownCompanyName) {
        String high = classifyHighConfidence(summary, remark, debit, credit, counterpartyName, ownCompanyName);
        if (high != null) {
            return high;
        }
        // 对公/扫码：贷方 + 对方非本公司 / 清算通道 / 企业户名
        if (isCorporateReceipt(summary, remark, null, counterpartyName, credit, ownCompanyName)) {
            return RECEIPT;
        }
        String s = nullToEmpty(summary);
        String r = nullToEmpty(remark);
        boolean isDebit = debit != null && debit.compareTo(BigDecimal.ZERO) > 0;
        boolean isCredit = credit != null && credit.compareTo(BigDecimal.ZERO) > 0;

        if (isDebit && s.contains("支付")) {
            return GOODS;
        }
        // 借方支出且备注有业务含义 → 默认货款；纯通道类备注不猜
        if (isDebit && StringUtils.hasText(r) && !r.contains("对公人民币") && !r.contains("转账")) {
            return GOODS;
        }
        if (isCredit && StringUtils.hasText(r) && (r.contains("货款") || r.contains("租金")
                || r.contains("回款") || r.contains("收款")
                || isQrReceiptSignal(summary, remark, null, counterpartyName)
                || isCorporateTransferReceiptSignal(summary, remark, null))) {
            return RECEIPT;
        }
        // 无法可靠匹配科目内容 → 其他（不写入）
        return OTHER;
    }

    /**
     * 对公收款通道摘要：大额/小额支付系统回单号（IBPS/HVPS/BEPS 等）。
     */
    public static boolean isCorporateTransferReceiptSignal(String summary, String remark, String purpose) {
        String text = (nullToEmpty(summary) + " " + nullToEmpty(remark) + " " + nullToEmpty(purpose)).toUpperCase();
        return text.contains("IBPS")
                || text.contains("HVPS")
                || text.contains("BEPS")
                || text.contains("CBPS")
                || text.contains("超级网银")
                || text.contains("普通汇兑")
                || text.contains("汇兑");
    }

    /**
     * 正常对公/扫码收款：贷方收入，且对方非本公司，或清算通道/扫码信号，或企业户名。
     */
    public static boolean isCorporateReceipt(String summary, String remark, String purpose,
                                             String counterpartyName, BigDecimal credit) {
        return isCorporateReceipt(summary, remark, purpose, counterpartyName, credit, null);
    }

    public static boolean isCorporateReceipt(String summary, String remark, String purpose,
                                             String counterpartyName, BigDecimal credit,
                                             String ownCompanyName) {
        if (credit == null || credit.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (isQrReceiptSignal(summary, remark, purpose, counterpartyName)
                || isCorporateTransferReceiptSignal(summary, remark, purpose)
                || isExternalReceiptPayer(counterpartyName, ownCompanyName)) {
            return true;
        }
        return looksLikeCompanyCounterparty(counterpartyName);
    }

    public static boolean looksLikeCompanyCounterparty(String counterpartyName) {
        String name = nullToEmpty(counterpartyName);
        if (!StringUtils.hasText(name)) {
            return false;
        }
        // 本公司名称本身不当作客户对公
        if (isOwnCompanyName(name, null)) {
            return false;
        }
        return name.contains("公司")
                || name.contains("有限")
                || name.contains("集团")
                || name.contains("厂")
                || name.contains("中心")
                || name.contains("事务所")
                || name.contains("合作社");
    }

    /**
     * 写入前解析最终类型。
     * 本地手续费信号可覆盖误反推的货款/运费；税费/个税等已保存类型不覆盖。
     * 已存「其他」时允许按对公收款规则重判（如 IBPS 贷方收入）。
     */
    public static String resolveWriteType(String storedType, String summary, String remark,
                                          BigDecimal debit, BigDecimal credit) {
        return resolveWriteType(storedType, summary, remark, debit, credit, null, null);
    }

    public static String resolveWriteType(String storedType, String summary, String remark,
                                          BigDecimal debit, BigDecimal credit, String counterpartyName) {
        return resolveWriteType(storedType, summary, remark, debit, credit, counterpartyName, null);
    }

    public static String resolveWriteType(String storedType, String summary, String remark,
                                          BigDecimal debit, BigDecimal credit, String counterpartyName,
                                          String ownCompanyName) {
        if (isLocalFeeSignal(summary, remark)) {
            String feeType = localFeeType(summary, remark);
            if (!StringUtils.hasText(storedType)
                    || GOODS.equals(storedType)
                    || FREIGHT.equals(storedType)
                    || OTHER.equals(storedType)
                    || FEE.equals(storedType)
                    || FINANCE_FEE.equals(storedType)) {
                return feeType;
            }
        }
        if (StringUtils.hasText(storedType) && !OTHER.equals(storedType.trim())) {
            return storedType.trim();
        }
        String high = classifyHighConfidence(summary, remark, debit, credit, counterpartyName, ownCompanyName);
        if (high != null) {
            return high;
        }
        return classify(summary, remark, debit, credit, counterpartyName, ownCompanyName);
    }

    /** 金蝶反推结果是否允许被本地手续费信号覆盖 */
    public static boolean canOverrideWithLocalFee(String kingdeeType) {
        return !StringUtils.hasText(kingdeeType)
                || GOODS.equals(kingdeeType)
                || FREIGHT.equals(kingdeeType)
                || OTHER.equals(kingdeeType);
    }

    /** 该类型写入是否必须对方户名（用于匹配供应商/客户/员工） */
    public static boolean requiresCounterparty(String type) {
        return GOODS.equals(type) || FREIGHT.equals(type) || EXPENSE.equals(type) || RECEIPT.equals(type);
    }

    /**
     * 扫码收款摘要已固定为「二维码收租金」，对方户名可为银行清算户或空，不强制要求客户户名。
     * 对公收到租金仍必须有付款人（对方户名）。
     */
    public static boolean requiresCounterparty(String type, String summary, String remark,
                                               String purpose, String counterpartyName) {
        if (!requiresCounterparty(type)) {
            return false;
        }
        if (RECEIPT.equals(type) && isQrReceiptSignal(summary, remark, purpose, counterpartyName)) {
            return false;
        }
        return true;
    }

    /**
     * 根据金蝶历史分录的对方科目代码 + 摘要反推银行流水凭证类型。
     */
    public static String inferFromKingdee(String accountCode, String explanation, String accountName) {
        String code = nullToEmpty(accountCode);
        String exp = nullToEmpty(explanation);
        String name = nullToEmpty(accountName);

        if (exp.contains("融资手续费") || (code.startsWith("6603.04") && exp.contains("融资"))) {
            return FINANCE_FEE;
        }
        // 税费科目优先于手续费，避免银行摘要「手续费」对应金蝶 2221 税费时被误判
        if ("2221.13".equals(code) || exp.contains("个税") || exp.contains("个人所得税")) {
            return PERSONAL_TAX;
        }
        if (code.startsWith("2221") || name.contains("应交税费") || name.contains("税费")
                || exp.contains("增值税") || exp.contains("城市维护")
                || exp.contains("教育费附加") || exp.contains("地方教育")
                || exp.contains("缴纳税费") || exp.contains("缴纳税款") || exp.contains("缴税")
                || exp.contains("纳税")) {
            return TAX;
        }
        if ("6603.04".equals(code) || (name.contains("财务费用") && name.contains("手续费"))) {
            return FEE;
        }
        if ("2202.02".equals(code) || name.contains("明细应付") || name.contains("应付账款")) {
            if (exp.contains("运费") || exp.contains("运输")) {
                return FREIGHT;
            }
            return GOODS;
        }
        if ("2241.03".equals(code)
                || exp.contains("报销")
                || exp.contains("差旅")
                || (name.contains("其他应付款") && (exp.contains("费用") || exp.contains("报销")))) {
            return EXPENSE;
        }
        if ("1122.01".equals(code) || name.contains("应收账款")
                || (exp.contains("收到") && (exp.contains("货款") || exp.contains("租金")))
                || exp.contains("二维码收租金")) {
            return RECEIPT;
        }
        if ("6603.02".equals(code) || exp.contains("结息") || name.contains("利息收入")) {
            return INTEREST;
        }
        if ("2203.02".equals(code) || code.startsWith("2203") || code.startsWith("2001")
                || name.contains("借款") || exp.contains("贷款") || exp.contains("放款")) {
            return LOAN;
        }
        if ("2211.06".equals(code) || exp.contains("工资") || name.contains("应付职工")) {
            return SALARY;
        }
        if (code.startsWith("2211") || exp.contains("社保") || exp.contains("养老") || exp.contains("医疗")
                || exp.contains("工伤") || exp.contains("失业")) {
            if (exp.contains("工资") || "2211.06".equals(code)) {
                return SALARY;
            }
            return SOCIAL;
        }
        if ("1121.02".equals(code) || exp.contains("银承") || exp.contains("票据包")) {
            return DISCOUNT_BA;
        }
        if ("1121.04".equals(code) || exp.contains("融单") || exp.contains("融资放款")) {
            return DISCOUNT_FIN;
        }
        if ("6603.07".equals(code) && (exp.contains("贴现") || name.contains("贴现"))) {
            return DISCOUNT_BA;
        }
        if (exp.contains("运费") || exp.contains("运输费")) {
            return FREIGHT;
        }
        if (exp.contains("手续费") || exp.contains("收费")) {
            return FEE;
        }
        if (exp.contains("支付") && (exp.contains("货款") || exp.contains("加工费"))) {
            return GOODS;
        }
        if (exp.contains("二维码收租金")
                || (exp.contains("收到") && (exp.contains("货款") || exp.contains("租金")))) {
            return RECEIPT;
        }
        if (exp.contains("报销") || exp.contains("费用")) {
            return EXPENSE;
        }
        return OTHER;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
