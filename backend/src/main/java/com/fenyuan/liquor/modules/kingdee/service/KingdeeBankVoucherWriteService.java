package com.fenyuan.liquor.modules.kingdee.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenyuan.liquor.modules.kingdee.config.KingdeeBankVoucherProperties;
import com.fenyuan.liquor.modules.kingdee.dto.KingdeeAccountContext;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankVoucherDetail;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherFeeAmounts;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherTypeClassifier;
import com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherSaveResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KingdeeBankVoucherWriteService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern TAX_ITEM_PATTERN = Pattern.compile("([^|\\d]+)(\\d+(?:\\.\\d+)?)");

    /**
     * 银字应付 2202.02 维度：同名多编码时金蝶列表首条为 1064（非末条 VEN00090）。
     * 仅此对手方特例，其它供应商仍走档案查询。
     */
    private static final String PAYABLE_SUPPLIER_XINYANG_LICHENG = "信阳利成环境科技有限公司";
    private static final String PAYABLE_SUPPLIER_XINYANG_LICHENG_NO = "1064";

    private final KingdeeBankVoucherDetailService detailService;
    private final KingdeeAccountResolverService accountResolverService;
    private final KingdeeLoginService loginService;
    private final KingdeeVoucherSaveService voucherSaveService;
    private final KingdeeVoucherQueryService voucherQueryService;
    private final KingdeeSupplierQueryService supplierQueryService;
    private final KingdeeBankVoucherProperties properties;
    private final KingdeeBankChannelService channelService;

    public KingdeeVoucherSaveResultVO writeSelected(List<Long> ids, Long accountId) {
        return writeSelected(ids, accountId, null);
    }

    public KingdeeVoucherSaveResultVO writeSelected(List<Long> ids, Long accountId, String channelKey) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("请选择要写入金蝶的明细");
        }
        List<KingdeeBankVoucherDetail> details = detailService.findByIds(ids);
        if (details.isEmpty()) {
            throw new RuntimeException("未找到选中的明细");
        }
        details.sort(Comparator
                .comparing(KingdeeBankVoucherDetail::getBookkeepingDate, Comparator.nullsLast(String::compareTo))
                .thenComparing(KingdeeBankVoucherDetail::getId));

        List<KingdeeBankVoucherDetail> writableDetails = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (KingdeeBankVoucherDetail detail : details) {
            // 平台已写入或查重已命中银字 → 不再重复写入
            if (isAlreadyInKingdee(detail)) {
                skipped.add(defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId()))
                        + "：金蝶已存在，跳过写入"
                        + existVoucherHint(detail));
                continue;
            }
            String type = BankVoucherTypeClassifier.resolveWriteType(
                    detail.getVoucherType(),
                    detail.getSummary(),
                    joinText(detail.getRemark(), detail.getPurpose()),
                    detail.getDebitAmount(),
                    detail.getCreditAmount(),
                    detail.getCounterpartyName(),
                    detail.getAccountName());
            detail.setVoucherType(type);
            detail.setVoucherTypeName(BankVoucherTypeClassifier.typeName(type));
            if (!BankVoucherTypeClassifier.isWritable(type)) {
                skipped.add(defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId()))
                        + "：" + BankVoucherTypeClassifier.nonWritableReason(type));
                continue;
            }
            if (BankVoucherTypeClassifier.requiresCounterparty(
                    type,
                    detail.getSummary(),
                    joinText(detail.getRemark(), detail.getPurpose()),
                    detail.getPurpose(),
                    detail.getCounterpartyName())
                    && !StringUtils.hasText(detail.getCounterpartyName())) {
                skipped.add(defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId()))
                        + "：" + BankVoucherTypeClassifier.typeName(type) + "需要对方户名，当前为空");
                continue;
            }
            writableDetails.add(detail);
        }
        if (writableDetails.isEmpty()) {
            boolean allExist = !skipped.isEmpty()
                    && skipped.stream().allMatch(s -> s.contains("金蝶已存在"));
            if (allExist) {
                KingdeeVoucherSaveResultVO result = new KingdeeVoucherSaveResultVO();
                result.setItemCount(details.size());
                result.setSuccessCount(0);
                result.setFailCount(0);
                result.setSkippedCount(skipped.size());
                result.setSavedMode("skipped");
                result.setMessage("全部为金蝶已存在凭证，已跳过写入（共 " + skipped.size() + " 条）。"
                        + String.join("；", skipped.size() > 5 ? skipped.subList(0, 5) : skipped)
                        + (skipped.size() > 5 ? "…" : ""));
                return result;
            }
            throw new RuntimeException("没有可写入的明细。"
                    + (skipped.isEmpty() ? "" : "已跳过：" + String.join("；", skipped)));
        }

        KingdeeBankChannel channel = channelService.resolveForOperation(channelKey, writableDetails);
        // 超管可指定账号；普通用户始终用绑定账号。账簿仍取自渠道配置
        KingdeeAccountContext account = accountResolverService.resolveForOperation(accountId);
        String orgCompanyCode = channelService.resolveOrgCompanyCode(channel, account);
        try {
            String sessionId = loginService.login(
                    account.getKingdeeUrl(), account.getDbId(), account.getUsername(), account.getPassword());
            String formId = properties.getFormId();
            List<String> voucherGroupCandidates = resolveVoucherGroupCandidates(
                    account.getKingdeeUrl(), sessionId, formId, orgCompanyCode);
            log.warn("写入金蝶账簿={} 凭证字候选={}", orgCompanyCode, voucherGroupCandidates);

            // 一行一银字；仅扫码收款摘要 FEE 在同一张凭证内拆分录（见 appendReceipt）
            int successCount = 0;
            int voucherCount = 0;
            String lastVoucherId = null;
            String lastVoucherNo = null;
            String firstExplanation = null;
            String usedVoucherGroup = properties.getVoucherGroup();
            List<String> errors = new ArrayList<>();

            for (KingdeeBankVoucherDetail detail : writableDetails) {
                String dateKey = detail.getBookkeepingDate();
                if (!StringUtils.hasText(dateKey)) {
                    throw new RuntimeException("明细缺少记账日期，id=" + detail.getId());
                }
                String serialLabel = defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId()));
                List<Long> lineIds = Collections.singletonList(detail.getId());
                try {
                    JSONArray entity = new JSONArray();
                    appendEntries(entity, detail, account, sessionId, orgCompanyCode, channel);
                    if (entity.isEmpty()) {
                        throw new RuntimeException("未生成任何分录");
                    }
                    assertVoucherBalanced(entity, serialLabel);
                    String explanation = entity.getJSONObject(0).getString("FEXPLANATION");
                    if (firstExplanation == null) {
                        firstExplanation = explanation;
                    }
                    LocalDate voucherDate = parseBookkeepingDate(dateKey);
                    KingdeeVoucherSaveResultVO lineResult = saveWithVoucherGroupFallback(
                            account.getKingdeeUrl(), sessionId, formId, voucherDate, orgCompanyCode,
                            entity, explanation, voucherGroupCandidates);
                    usedVoucherGroup = lineResult.getVoucherGroup() != null
                            ? lineResult.getVoucherGroup() : usedVoucherGroup;
                    lineResult.setSavedMode("created");
                    detailService.markWritten(lineIds, lineResult.getVoucherId(), lineResult.getVoucherNumber(),
                            lineResult.getVoucherGroup(), account.getId());
                    successCount++;
                    voucherCount++;
                    lastVoucherId = lineResult.getVoucherId();
                    lastVoucherNo = lineResult.getVoucherNumber();
                } catch (Exception e) {
                    log.error("写入金蝶失败 serial={} date={} org={}", serialLabel, dateKey, orgCompanyCode, e);
                    detailService.markWriteError(lineIds, e.getMessage());
                    errors.add(serialLabel + "：" + e.getMessage());
                }
            }

            KingdeeVoucherSaveResultVO result = new KingdeeVoucherSaveResultVO();
            result.setVoucherId(lastVoucherId);
            result.setVoucherNumber(lastVoucherNo);
            result.setExplanation(firstExplanation);
            result.setVoucherGroup(usedVoucherGroup);
            result.setItemCount(details.size());
            result.setSuccessCount(successCount);
            result.setFailCount(Math.max(0, writableDetails.size() - successCount));
            result.setSkippedCount(skipped.size());
            result.setSavedMode(voucherCount > 1 ? "batched" : "created");
            List<String> messages = new ArrayList<>();
            if (successCount > 0) {
                messages.add("写入成功 " + successCount + " 条流水 / " + voucherCount
                        + " 张银字凭证（货款与手续费分行；扫码摘要FEE同凭证拆分录；账套组织 " + orgCompanyCode
                        + " / " + channel.getCompanyName() + channel.getBankName() + "）");
            }
            if (!skipped.isEmpty()) {
                messages.add("跳过不可写入 " + skipped.size() + " 条（含金蝶已存在/社保/工资/贴现/其他）");
            }
            if (!errors.isEmpty()) {
                messages.add("失败：" + String.join("；", errors));
            }
            result.setMessage(String.join("；", messages));
            if (successCount == 0 && !errors.isEmpty()) {
                throw new RuntimeException(result.getMessage());
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("写入金蝶失败：" + e.getMessage(), e);
        }
    }

    private void appendEntries(JSONArray entity, KingdeeBankVoucherDetail detail,
                               KingdeeAccountContext account, String sessionId, String orgCompanyCode,
                               KingdeeBankChannel channel) {
        String type = BankVoucherTypeClassifier.resolveWriteType(
                detail.getVoucherType(),
                detail.getSummary(),
                joinText(detail.getRemark(), detail.getPurpose()),
                detail.getDebitAmount(),
                detail.getCreditAmount(),
                detail.getCounterpartyName(),
                detail.getAccountName());
        detail.setVoucherType(type);
        detail.setVoucherTypeName(BankVoucherTypeClassifier.typeName(type));
        if (!BankVoucherTypeClassifier.isWritable(type)) {
            throw new RuntimeException(BankVoucherTypeClassifier.nonWritableReason(type)
                    + "（流水：" + defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())) + "）");
        }
        switch (type) {
            case BankVoucherTypeClassifier.FEE:
            case BankVoucherTypeClassifier.FINANCE_FEE:
                appendFee(entity, detail);
                break;
            case BankVoucherTypeClassifier.GOODS:
            case BankVoucherTypeClassifier.FREIGHT:
                appendPayable(entity, detail, account, sessionId, type, orgCompanyCode);
                break;
            case BankVoucherTypeClassifier.EXPENSE:
                appendExpense(entity, detail, account, sessionId);
                break;
            case BankVoucherTypeClassifier.PERSONAL_TAX:
                appendPersonalTax(entity, detail);
                break;
            case BankVoucherTypeClassifier.TAX:
                appendTax(entity, detail);
                break;
            case BankVoucherTypeClassifier.RECEIPT:
                appendReceipt(entity, detail, account, sessionId, channel);
                break;
            case BankVoucherTypeClassifier.INTEREST:
                appendInterest(entity, detail);
                break;
            case BankVoucherTypeClassifier.LOAN:
                appendLoan(entity, detail);
                break;
            default:
                throw new RuntimeException("暂不支持的凭证类型："
                        + defaultText(detail.getVoucherTypeName(), type)
                        + "，流水：" + defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())));
        }
    }

    private void appendFee(JSONArray entity, KingdeeBankVoucherDetail detail) {
        BigDecimal amount = requireDebit(detail);
        String bank = bankLabelOf(detail);
        String explanation = StringUtils.hasText(detail.getCounterpartyName())
                ? bank + "支付" + detail.getCounterpartyName().trim()
                + (BankVoucherTypeClassifier.FINANCE_FEE.equals(detail.getVoucherType()) ? "融资手续费" : "手续费")
                : bank + "支付手续费";
        entity.add(buildDebitEntry(explanation, properties.getFeeAccount(), amount, null, null));
        entity.add(buildCreditEntry(explanation, bankAccountOf(detail), amount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
    }

    private void appendPayable(JSONArray entity, KingdeeBankVoucherDetail detail,
                               KingdeeAccountContext account, String sessionId, String type,
                               String orgCompanyCode) {
        BigDecimal amount = requireDebit(detail);
        String name = requireCounterparty(detail);
        // 供应商按「使用组织」查，不是账簿编码（002≠110）
        String useOrgCode = StringUtils.hasText(account.getUseOrgCode())
                ? account.getUseOrgCode().trim()
                : null;
        // 2202.02 应付维度：信阳利成同名多条时取列表第一个 1064
        String supplierNo = PAYABLE_SUPPLIER_XINYANG_LICHENG.equals(name.trim())
                ? PAYABLE_SUPPLIER_XINYANG_LICHENG_NO
                : supplierQueryService.findSupplierNumberByName(
                        account.getKingdeeUrl(), sessionId, name, useOrgCode);
        if (!StringUtils.hasText(supplierNo)) {
            throw new RuntimeException(supplierQueryService.describeMissingSupplier(
                    account.getKingdeeUrl(), sessionId, name, useOrgCode));
        }
        String label = BankVoucherTypeClassifier.FREIGHT.equals(type) ? "运费" : "货款";
        String explanation = "支付" + name + label;
        entity.add(buildDebitEntry(explanation, properties.getPayableAccount(), amount,
                properties.getSupplierDimensionKey(), supplierNo));
        entity.add(buildCreditEntry(explanation, bankAccountOf(detail), amount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
    }

    private void appendExpense(JSONArray entity, KingdeeBankVoucherDetail detail,
                               KingdeeAccountContext account, String sessionId) {
        BigDecimal amount = requireDebit(detail);
        String name = requireCounterparty(detail);
        String employeeNo = voucherQueryService.findEmployeeNumberByName(
                account.getKingdeeUrl(), sessionId, name);
        if (!StringUtils.hasText(employeeNo)) {
            throw new RuntimeException("未在金蝶员工档案找到「" + name + "」");
        }
        String explanation = "支付" + name + "报销费用";
        entity.add(buildDebitEntry(explanation, properties.getExpenseAccount(), amount,
                properties.getEmployeeDimensionKey(), employeeNo));
        entity.add(buildCreditEntry(explanation, bankAccountOf(detail), amount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
    }

    private void appendPersonalTax(JSONArray entity, KingdeeBankVoucherDetail detail) {
        BigDecimal amount = requireDebit(detail);
        String explanation = "缴纳个税";
        entity.add(buildDebitEntry(explanation, properties.getPersonalTaxAccount(), amount, null, null));
        entity.add(buildCreditEntry(explanation, bankAccountOf(detail), amount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
    }

    private void appendTax(JSONArray entity, KingdeeBankVoucherDetail detail) {
        BigDecimal bankAmount = requireDebit(detail);
        String remark = defaultText(detail.getRemark(), "");
        List<TaxItem> items;
        try {
            items = parseTaxItems(remark);
        } catch (RuntimeException ex) {
            // 中行「实时缴税」备注多为税票流水号，无法按税种拆分 → 整笔记应交税费
            log.warn("税费备注无法拆分，整笔入账 serial={} reason={}",
                    defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())),
                    ex.getMessage());
            items = Collections.emptyList();
        }
        if (items.isEmpty()) {
            String explanation = "缴纳税费";
            entity.add(buildDebitEntry(explanation, properties.getVatTaxAccount(), bankAmount, null, null));
            entity.add(buildCreditEntry(explanation, bankAccountOf(detail), bankAmount,
                    properties.getBankDimensionKey(), bankDimensionOf(detail)));
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (TaxItem item : items) {
            sum = sum.add(item.amount);
            entity.add(buildDebitEntry(item.explanation, item.account, item.amount, null, null));
        }
        sum = sum.setScale(2, RoundingMode.HALF_UP);
        if (sum.compareTo(bankAmount) != 0) {
            throw new RuntimeException("税费拆分合计 " + sum.toPlainString()
                    + " 与银行支出 " + bankAmount.toPlainString() + " 不一致，备注：" + remark);
        }
        entity.add(buildCreditEntry("缴纳各种税费", bankAccountOf(detail), bankAmount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
    }

    private void appendReceipt(JSONArray entity, KingdeeBankVoucherDetail detail,
                               KingdeeAccountContext account, String sessionId,
                               KingdeeBankChannel channel) {
        BigDecimal bankAmount = requireCredit(detail);
        boolean qr = BankVoucherTypeClassifier.isQrReceiptSignal(
                detail.getSummary(), detail.getRemark(), detail.getPurpose(),
                detail.getCounterpartyName());
        // 对公收款：收到+付款人+租金；扫码：二维码收租金
        String explanation = qr ? "二维码收货款" : ("收到" + requireCounterparty(detail) + "货款");
        String customerNo = resolveReceiptCustomerNo(detail, account, sessionId, channel, qr);
        if (!StringUtils.hasText(customerNo)) {
            String useOrgCode = StringUtils.hasText(account.getUseOrgCode())
                    ? account.getUseOrgCode().trim() : null;
            String orgHint = resolveBankCustomerOrgNameHint(account, channel);
            String lookupName = qr
                    ? defaultText(properties.getQrCustomerName(), "扫码收款")
                    : detail.getCounterpartyName();
            throw new RuntimeException(voucherQueryService.describeMissingCustomer(
                    account.getKingdeeUrl(), sessionId, lookupName, useOrgCode, orgHint));
        }

        if (qr) {
            boolean feeExempt = channelService.isQrFeeExempt(channel);
            BigDecimal feeFromSummary = BankVoucherFeeAmounts.parseFeeFromText(
                    detail.getSummary(), detail.getRemark(), detail.getPurpose());
            // 农商行等免手续费渠道：借银行存款 / 贷扫码收款，不按费率反推；仅当摘要显式含 FEE 时才拆
            BigDecimal feeRate = feeExempt ? BigDecimal.ZERO : parseQrFeeRate();
            if (feeExempt && feeFromSummary == null) {
                log.info("扫码收款免手续费（渠道） serial={} 到账={}",
                        defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())),
                        bankAmount.toPlainString());
            } else {
                BigDecimal gross = BankVoucherFeeAmounts.resolveQrGrossAmount(
                        bankAmount,
                        detail.getSummary(), detail.getRemark(), detail.getPurpose(),
                        feeRate);
                BigDecimal fee = BankVoucherFeeAmounts.resolveQrFeeAmount(bankAmount, gross);
                if (fee.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("扫码收款拆分手续费 serial={} 到账={} FEE摘要={} 实付={} feeExempt={}",
                            defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())),
                            bankAmount.toPlainString(),
                            feeFromSummary != null ? feeFromSummary.toPlainString() : fee.toPlainString(),
                            gross.toPlainString(),
                            feeExempt);
                    entity.add(buildDebitEntry(explanation, bankAccountOf(detail), bankAmount,
                            properties.getBankDimensionKey(), bankDimensionOf(detail)));
                    entity.add(buildFeeMarkerEntry(explanation + "手续费", properties.getFeeAccount(), fee));
                    entity.add(buildCreditEntry(explanation, properties.getReceivableAccount(), gross,
                            properties.getCustomerDimensionKey(), customerNo));
                    return;
                }
            }
        }

        // 微信/扫码（含农商行免手续费）：借银行存款，贷扫码收款（应收账款-扫码客户）
        entity.add(buildDebitEntry(explanation, bankAccountOf(detail), bankAmount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
        entity.add(buildCreditEntry(explanation, properties.getReceivableAccount(), bankAmount,
                properties.getCustomerDimensionKey(), customerNo));
    }

    /**
     * 对公：按对方户名找客户；扫码：优先对方户名，清算户/空则用配置的扫码默认客户。
     * 组织匹配：编码 + 组织名称（汾源），避免账簿编码与客户使用组织编码不一致时误杀。
     * 扫码清算客户允许租户内共享（CUST0004 等）。
     */
    private String resolveReceiptCustomerNo(KingdeeBankVoucherDetail detail,
                                            KingdeeAccountContext account, String sessionId,
                                            KingdeeBankChannel channel, boolean qr) {
        String useOrgCode = StringUtils.hasText(account.getUseOrgCode())
                ? account.getUseOrgCode().trim() : null;
        String orgNameHint = resolveBankCustomerOrgNameHint(account, channel);
        String name = trimToNull(detail.getCounterpartyName());
        if (StringUtils.hasText(name)
                && !BankVoucherTypeClassifier.isQrSettlementCounterparty(name)
                && !BankVoucherTypeClassifier.isOwnCompanyName(name, detail.getAccountName())) {
            String no = voucherQueryService.findCustomerNumberByName(
                    account.getKingdeeUrl(), sessionId, name, useOrgCode, orgNameHint);
            if (StringUtils.hasText(no)) {
                return no;
            }
            if (!qr) {
                return null;
            }
        }
        if (qr) {
            for (String fallback : qrCustomerNameCandidates()) {
                String no = voucherQueryService.findCustomerNumberByName(
                        account.getKingdeeUrl(), sessionId, fallback, useOrgCode, orgNameHint);
                if (StringUtils.hasText(no)) {
                    log.info("扫码收款使用客户档案「{}」→ {}", fallback, no);
                    return no;
                }
            }
        }
        return null;
    }

    private List<String> qrCustomerNameCandidates() {
        List<String> names = new ArrayList<>();
        String configured = trimToNull(properties.getQrCustomerName());
        if (configured != null) {
            names.add(configured);
        }
        for (String n : new String[]{"扫码收款", "银联扫码", "二维码收款", "微信收款", "商户清算"}) {
            if (!names.contains(n)) {
                names.add(n);
            }
        }
        return names;
    }

    private String resolveBankCustomerOrgNameHint(KingdeeAccountContext account, KingdeeBankChannel channel) {
        if (channel != null && StringUtils.hasText(channel.getAccountName())) {
            return channel.getAccountName().trim();
        }
        if (channel != null && StringUtils.hasText(channel.getCompanyName())) {
            return channel.getCompanyName().trim();
        }
        if (account != null && StringUtils.hasText(account.getAccountName())) {
            return account.getAccountName().trim();
        }
        return "广东汾源酒业";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private BigDecimal parseQrFeeRate() {
        try {
            if (StringUtils.hasText(properties.getQrFeeRate())) {
                return new BigDecimal(properties.getQrFeeRate().trim());
            }
        } catch (Exception ignored) {
            // fall through
        }
        return new BigDecimal("0.003");
    }

    /** 手续费独立分录（不并入主交易摘要；F1 业务标识写入分录扩展字段） */
    private JSONObject buildFeeMarkerEntry(String explanation, String account, BigDecimal amount) {
        JSONObject entry = buildDebitEntry(explanation, account, amount, null, null);
        entry.put("F1", "手续费");
        return entry;
    }

    private void appendInterest(JSONArray entity, KingdeeBankVoucherDetail detail) {
        BigDecimal amount = requireCredit(detail);
        String explanation = bankLabelOf(detail) + "结息";
        entity.add(buildDebitEntry(explanation, bankAccountOf(detail), amount,
                properties.getBankDimensionKey(), bankDimensionOf(detail)));
        entity.add(buildCreditEntry(explanation, properties.getInterestIncomeAccount(), amount, null, null));
    }

    private void appendLoan(JSONArray entity, KingdeeBankVoucherDetail detail) {
        BigDecimal debit = detail.getDebitAmount();
        BigDecimal credit = detail.getCreditAmount();
        boolean outflow = debit != null && debit.compareTo(BigDecimal.ZERO) > 0;
        boolean inflow = credit != null && credit.compareTo(BigDecimal.ZERO) > 0;
        String explanation = StringUtils.hasText(detail.getCounterpartyName())
                ? detail.getCounterpartyName().trim()
                : "贷款相关";
        if (outflow) {
            BigDecimal amount = debit.setScale(2, RoundingMode.HALF_UP);
            entity.add(buildDebitEntry(explanation, properties.getLoanAccount(), amount, null, null));
            entity.add(buildCreditEntry(explanation, bankAccountOf(detail), amount,
                    properties.getBankDimensionKey(), bankDimensionOf(detail)));
        } else if (inflow) {
            BigDecimal amount = credit.setScale(2, RoundingMode.HALF_UP);
            entity.add(buildDebitEntry(explanation, bankAccountOf(detail), amount,
                    properties.getBankDimensionKey(), bankDimensionOf(detail)));
            entity.add(buildCreditEntry(explanation, properties.getLoanAccount(), amount, null, null));
        } else {
            throw new RuntimeException("贷款相关借贷金额无效，流水："
                    + defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())));
        }
    }

    /**
     * 按备注拆分税费：支持「税种金额|税种金额」格式，全部税种均需可映射科目。
     * 例：教育费附加2117.99|印花税55882.86|增值税70599.63|城市维护建设税4941.97|地方教育附加1411.99
     */
    private List<TaxItem> parseTaxItems(String remark) {
        List<TaxItem> items = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();
        if (!StringUtils.hasText(remark)) {
            return items;
        }
        String[] segments = remark.split("[|｜]");
        for (String segment : segments) {
            String part = segment == null ? "" : segment.trim();
            if (!StringUtils.hasText(part)) {
                continue;
            }
            Matcher matcher = TAX_ITEM_PATTERN.matcher(part);
            if (!matcher.find()) {
                unmapped.add(part + "（无法解析金额）");
                continue;
            }
            String name = matcher.group(1).trim().replaceAll("^[\\s,，;；]+|[\\s,，;；]+$", "");
            BigDecimal amount = new BigDecimal(matcher.group(2)).setScale(2, RoundingMode.HALF_UP);
            String account = mapTaxAccount(name);
            if (account == null) {
                unmapped.add(name);
                continue;
            }
            TaxItem item = new TaxItem();
            item.explanation = buildTaxExplanation(name);
            item.account = account;
            item.amount = amount;
            items.add(item);
        }
        if (!unmapped.isEmpty()) {
            throw new RuntimeException("税费备注存在未映射科目：" + String.join("、", unmapped)
                    + "。备注：" + remark);
        }
        return items;
    }

    private String buildTaxExplanation(String name) {
        if (name.contains("增值税")) {
            return "缴纳增值税";
        }
        if (name.contains("城市维护")) {
            return "缴纳城市维护建设税";
        }
        if (name.contains("地方教育")) {
            return "缴纳地方教育附加税";
        }
        if (name.contains("教育费")) {
            return "缴纳教育费附加税";
        }
        if (name.contains("个人所得") || name.contains("个税")) {
            return "缴纳个税";
        }
        if (name.contains("印花")) {
            return "缴纳印花税";
        }
        if (name.contains("税")) {
            return "缴纳" + name;
        }
        return "缴纳" + name;
    }

    private String mapTaxAccount(String name) {
        if (name.contains("增值税")) {
            return properties.getVatTaxAccount();
        }
        if (name.contains("城市维护")) {
            return properties.getUrbanTaxAccount();
        }
        if (name.contains("地方教育")) {
            return properties.getLocalEducationSurchargeAccount();
        }
        if (name.contains("教育费")) {
            return properties.getEducationSurchargeAccount();
        }
        if (name.contains("个人所得") || name.contains("个税")) {
            return properties.getPersonalTaxAccount();
        }
        if (name.contains("印花")) {
            return properties.getStampTaxAccount();
        }
        return null;
    }

    private JSONObject buildNewVoucherModel(LocalDate voucherDate, String orgCompanyCode,
                                            String voucherGroupNumber, JSONArray entity) {
        if (!StringUtils.hasText(voucherGroupNumber)) {
            throw new RuntimeException("凭证字编码为空，无法写入金蝶");
        }
        JSONObject model = new JSONObject();
        // 不传 FVOUCHERID，避免被当成修改已有凭证
        model.put("FAccountBookID", buildNumberObject(orgCompanyCode));
        model.put("FDate", voucherDate.format(DATE_FORMATTER));
        model.put("FYEAR", String.valueOf(voucherDate.getYear()));
        model.put("FPERIOD", String.valueOf(voucherDate.getMonthValue()));
        model.put("FVOUCHERGROUPID", buildNumberObject(voucherGroupNumber.trim()));
        model.put("FEntity", entity);
        return model;
    }

    private List<String> resolveVoucherGroupCandidates(String kingdeeUrl, String sessionId,
                                                       String formId, String orgCompanyCode) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        // 银行流水固定写「银」字；勿回退到记/收/付，避免账簿配错时误报「凭证字必填」且试遍全部字
        String groupName = StringUtils.hasText(properties.getVoucherGroup())
                ? properties.getVoucherGroup().trim() : "银";

        // 1) 该账簿已有银字凭证上的真实编码（优先，如 PZZ2）
        String fromBook = voucherQueryService.findVoucherGroupNumberFromAccountBook(
                kingdeeUrl, sessionId, formId, orgCompanyCode, groupName);
        if (StringUtils.hasText(fromBook)) {
            candidates.add(fromBook.trim());
        }

        // 2) 基础资料按名称「银」查编码
        String byName = voucherQueryService.findVoucherGroupNumberByName(kingdeeUrl, sessionId, groupName);
        if (StringUtils.hasText(byName)) {
            candidates.add(byName.trim());
        }

        // 3) 配置兜底
        if (StringUtils.hasText(properties.getVoucherGroupNumber())) {
            candidates.add(properties.getVoucherGroupNumber().trim());
        }
        candidates.add("PZZ2");

        List<String> labels = voucherQueryService.listVoucherGroupLabels(kingdeeUrl, sessionId);
        if (!labels.isEmpty()) {
            log.warn("金蝶凭证字档案：{}", labels);
            for (String label : labels) {
                int eq = label.indexOf('=');
                if (eq > 0) {
                    String number = label.substring(0, eq).trim();
                    String name = label.substring(eq + 1).trim();
                    if (groupName.equals(name)) {
                        candidates.add(number);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new RuntimeException("未找到可用凭证字「" + groupName + "」，请检查金蝶基础资料-凭证字，或配置 "
                    + "kingdee.voucher.bank-detail.voucher-group-number（汾源账簿一般为 PZZ2）");
        }
        return new ArrayList<>(candidates);
    }

    private KingdeeVoucherSaveResultVO saveWithVoucherGroupFallback(
            String kingdeeUrl, String sessionId, String formId, LocalDate voucherDate,
            String orgCompanyCode, JSONArray entity, String explanation,
            List<String> voucherGroupCandidates) {
        RuntimeException lastError = null;
        for (String groupNumber : voucherGroupCandidates) {
            if (!StringUtils.hasText(groupNumber)) {
                continue;
            }
            JSONObject model = buildNewVoucherModel(voucherDate, orgCompanyCode, groupNumber, entity);
            log.warn("尝试写入金蝶 账簿={} 凭证字编码={} FVOUCHERGROUPID={}",
                    orgCompanyCode, groupNumber, model.getJSONObject("FVOUCHERGROUPID"));
            try {
                KingdeeVoucherSaveResultVO dayResult = voucherSaveService.save(
                        kingdeeUrl, sessionId, formId, model, explanation, groupNumber, true);
                log.warn("写入金蝶成功 账簿={} 凭证字编码={}", orgCompanyCode, groupNumber);
                return dayResult;
            } catch (RuntimeException e) {
                lastError = e;
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("凭证字")) {
                    log.warn("凭证字编码 {} 无效，尝试下一个：{}", groupNumber, msg);
                    continue;
                }
                throw e;
            }
        }
        throw lastError != null ? lastError
                : new RuntimeException("写入金蝶失败：无可用凭证字编码，已尝试 " + voucherGroupCandidates);
    }

    private JSONObject buildDebitEntry(String explanation, String account, BigDecimal amount,
                                       String dimensionKey, String dimensionNumber) {
        JSONObject entry = new JSONObject();
        entry.put("FEntryID", 0);
        entry.put("FEXPLANATION", explanation);
        entry.put("FACCOUNTID", buildNumberObject(account));
        applyCurrencyFields(entry);
        entry.put("FDEBIT", amountValue(amount));
        entry.put("FCREDIT", 0);
        applyDimension(entry, dimensionKey, dimensionNumber);
        return entry;
    }

    private JSONObject buildCreditEntry(String explanation, String account, BigDecimal amount,
                                        String dimensionKey, String dimensionNumber) {
        JSONObject entry = new JSONObject();
        entry.put("FEntryID", 0);
        entry.put("FEXPLANATION", explanation);
        entry.put("FACCOUNTID", buildNumberObject(account));
        applyCurrencyFields(entry);
        entry.put("FDEBIT", 0);
        entry.put("FCREDIT", amountValue(amount));
        applyDimension(entry, dimensionKey, dimensionNumber);
        return entry;
    }

    /** 借贷金额统一为数值，避免字符串/整数混用导致金蝶判定不平衡 */
    private double amountValue(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void assertVoucherBalanced(JSONArray entity, String dateKey) {
        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;
        for (int i = 0; i < entity.size(); i++) {
            JSONObject row = entity.getJSONObject(i);
            debitSum = debitSum.add(toAmount(row.get("FDEBIT")));
            creditSum = creditSum.add(toAmount(row.get("FCREDIT")));
        }
        debitSum = debitSum.setScale(2, RoundingMode.HALF_UP);
        creditSum = creditSum.setScale(2, RoundingMode.HALF_UP);
        if (debitSum.compareTo(creditSum) != 0) {
            throw new RuntimeException("本地分录不平衡：借方 " + debitSum.toPlainString()
                    + " ≠ 贷方 " + creditSum.toPlainString()
                    + "，日期 " + dateKey + "，分录数 " + entity.size());
        }
    }

    private BigDecimal toAmount(Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw instanceof Number) {
            return BigDecimal.valueOf(((Number) raw).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
    }

    private void applyDimension(JSONObject entry, String dimensionKey, String dimensionNumber) {
        if (StringUtils.hasText(dimensionKey) && StringUtils.hasText(dimensionNumber)) {
            JSONObject detailId = new JSONObject();
            detailId.put(dimensionKey, buildNumberObject(dimensionNumber));
            entry.put("FDetailID", detailId);
        }
    }

    private void applyCurrencyFields(JSONObject entry) {
        entry.put("FCURRENCYID", buildNumberObject(properties.getCurrencyId()));
        entry.put("FEXCHANGERATETYPE", buildNumberObject(properties.getExchangeRateType()));
        entry.put("FEXCHANGERATE", properties.getExchangeRate());
    }

    private JSONObject buildNumberObject(String number) {
        JSONObject obj = new JSONObject();
        obj.put("FNumber", number);
        return obj;
    }

    private String bankAccountOf(KingdeeBankVoucherDetail detail) {
        return channelService.resolveBankAccountForDetail(detail);
    }

    private String bankDimensionOf(KingdeeBankVoucherDetail detail) {
        return channelService.resolveBankDimensionForDetail(detail);
    }

    private String bankLabelOf(KingdeeBankVoucherDetail detail) {
        return channelService.bankLabel(channelService.resolveForDetail(detail));
    }

    private BigDecimal requireDebit(KingdeeBankVoucherDetail detail) {
        if (detail.getDebitAmount() == null || detail.getDebitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("借方金额无效，流水："
                    + defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())));
        }
        return detail.getDebitAmount().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal requireCredit(KingdeeBankVoucherDetail detail) {
        if (detail.getCreditAmount() == null || detail.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("贷方金额无效，流水："
                    + defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())));
        }
        return detail.getCreditAmount().setScale(2, RoundingMode.HALF_UP);
    }

    /** 仅查重「金蝶已有」跳过；查重「金蝶无」允许重新写入 */
    private boolean isAlreadyInKingdee(KingdeeBankVoucherDetail detail) {
        if (detail == null) {
            return false;
        }
        if (detail.getKingdeeExistStatus() != null && detail.getKingdeeExistStatus() == 2) {
            return false;
        }
        if (detail.getKingdeeExistStatus() != null && detail.getKingdeeExistStatus() == 1) {
            return true;
        }
        if (StringUtils.hasText(detail.getKingdeeExistVoucherNo())) {
            return true;
        }
        // 未查重时：平台已成功写入仍跳过，避免重复推送
        return detail.getWriteStatus() != null && detail.getWriteStatus() == 1
                && StringUtils.hasText(detail.getKingdeeVoucherId())
                && !detail.getKingdeeVoucherId().startsWith("EXIST-");
    }

    private String existVoucherHint(KingdeeBankVoucherDetail detail) {
        if (detail == null) {
            return "";
        }
        String word = null;
        String no = null;
        if (StringUtils.hasText(detail.getKingdeeVoucherNo())) {
            word = detail.getKingdeeVoucherWord();
            no = detail.getKingdeeVoucherNo();
        } else if (StringUtils.hasText(detail.getKingdeeExistVoucherNo())) {
            word = detail.getKingdeeExistVoucherWord();
            no = detail.getKingdeeExistVoucherNo();
        }
        if (!StringUtils.hasText(no)) {
            return "";
        }
        return "（" + defaultText(word, properties.getVoucherGroup()) + "-" + no + "）";
    }

    private String requireCounterparty(KingdeeBankVoucherDetail detail) {
        if (!StringUtils.hasText(detail.getCounterpartyName())) {
            throw new RuntimeException("对方户名为空，流水："
                    + defaultText(detail.getTradeSerialNo(), String.valueOf(detail.getId())));
        }
        return detail.getCounterpartyName().trim();
    }

    private LocalDate parseBookkeepingDate(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            return LocalDate.of(
                    Integer.parseInt(digits.substring(0, 4)),
                    Integer.parseInt(digits.substring(4, 6)),
                    Integer.parseInt(digits.substring(6, 8)));
        }
        return LocalDate.parse(raw);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String joinText(String... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                if (!StringUtils.hasText(part)) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }

    private static class TaxItem {
        private String explanation;
        private String account;
        private BigDecimal amount;
    }
}
