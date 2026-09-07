package com.fenyuan.liquor.modules.kingdee.service;

import com.fenyuan.liquor.modules.kingdee.config.KingdeeBankVoucherProperties;
import com.fenyuan.liquor.modules.kingdee.dto.KingdeeAccountContext;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankVoucherDetail;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeBankVoucherDetailMapper;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherTypeClassifier;
import com.fenyuan.liquor.modules.kingdee.vo.BankVoucherTypeInferResultVO;
import com.fenyuan.liquor.modules.kingdee.vo.VoucherDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 拉取金蝶历史凭证，按「同日期 + 同金额」银行科目分录反推银行明细的写入类型。
 * <p>
 * 建行明细借方（支出）↔ 金蝶银行科目贷方；建行明细贷方（收入）↔ 金蝶银行科目借方。
 * 优先据金蝶对方科目（如 2221 税费）反推；未命中时再用本地摘要兜底。
 * 类型确定后同步预检客户/员工/供应商档案是否齐全（与写入金蝶同一套查找逻辑），缺档标为不可写。
 * 反推不标记「金蝶已有」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankVoucherTypeInferService {

    private static final String PAYABLE_SUPPLIER_XINYANG_LICHENG = "信阳利成环境科技有限公司";

    private final KingdeeBankVoucherDetailService detailService;
    private final KingdeeBankVoucherDetailMapper detailMapper;
    private final KingdeeAccountResolverService accountResolverService;
    private final KingdeeLoginService loginService;
    private final KingdeeVoucherQueryService voucherQueryService;
    private final KingdeeSupplierQueryService supplierQueryService;
    private final KingdeeBankVoucherProperties properties;
    private final KingdeeBankChannelService channelService;

    @Transactional
    public BankVoucherTypeInferResultVO infer(List<Long> ids, Long accountId,
                                              List<String> accountingPeriods, boolean dryRun) {
        return infer(ids, accountId, null, accountingPeriods, dryRun);
    }

    @Transactional
    public BankVoucherTypeInferResultVO infer(List<Long> ids, Long accountId, String channelKey,
                                              List<String> accountingPeriods, boolean dryRun) {
        List<KingdeeBankVoucherDetail> details;
        if (ids != null && !ids.isEmpty()) {
            details = detailService.findByIds(ids);
        } else {
            details = detailService.search(channelKey, null, null, null, null, null, null).stream()
                    .filter(d -> d.getWriteStatus() == null || d.getWriteStatus() != 1)
                    .collect(Collectors.toList());
        }
        if (details.isEmpty()) {
            throw new RuntimeException("没有可反推的银行明细");
        }

        KingdeeBankChannel channel = channelService.resolveForOperation(channelKey, details);
        KingdeeAccountContext account = accountResolverService.resolveForOperation(accountId);
        String orgCompanyCode = channelService.resolveOrgCompanyCode(channel, account);
        String formId = StringUtils.hasText(properties.getFormId())
                ? properties.getFormId() : "GL_VOUCHER";

        Set<String> periods = new LinkedHashSet<>();
        if (accountingPeriods != null) {
            for (String p : accountingPeriods) {
                if (StringUtils.hasText(p) && p.trim().replaceAll("\\D", "").length() >= 6) {
                    String digits = p.trim().replaceAll("\\D", "");
                    periods.add(digits.substring(0, 6));
                }
            }
        }
        for (KingdeeBankVoucherDetail d : details) {
            for (String dateKey : candidateDates(d)) {
                String period = toAccountingPeriod(dateKey);
                if (period != null) {
                    periods.add(period);
                    periods.add(shiftPeriod(period, -1));
                    periods.add(shiftPeriod(period, 1));
                }
            }
        }
        if (periods.isEmpty()) {
            throw new RuntimeException("明细缺少记账日期，无法确定会计期间");
        }

        final String sessionId;
        try {
            sessionId = loginService.login(
                    account.getKingdeeUrl(), account.getDbId(), account.getUsername(), account.getPassword());
        } catch (java.io.IOException e) {
            throw new RuntimeException("登录金蝶失败：" + e.getMessage(), e);
        }

        List<VoucherDetailVO> allEntries = new ArrayList<>();
        List<String> queried = new ArrayList<>();
        for (String period : periods) {
            try {
                List<VoucherDetailVO> rows = voucherQueryService.queryVoucherDetails(
                        account.getKingdeeUrl(), sessionId, formId, period,
                        null, null, orgCompanyCode, KingdeeVoucherQueryService.ORG_FIELD_KEY,
                        true);
                if (rows != null) {
                    allEntries.addAll(rows);
                }
                queried.add(period);
            } catch (Exception e) {
                log.warn("查询金蝶凭证失败 period={} org={}：{}", period, orgCompanyCode, e.getMessage());
            }
        }

        Map<String, List<VoucherDetailVO>> voucherGroups = groupByVoucher(allEntries);
        // 索引键：科目|维度|日期|方向|金额
        Map<String, List<BankHit>> bankIndex = buildBankIndex(allEntries);

        BankVoucherTypeInferResultVO result = new BankVoucherTypeInferResultVO();
        result.setTotal(details.size());
        result.setPeriodsQueried(queried);
        result.setKingdeeEntryCount(allEntries.size());

        for (KingdeeBankVoucherDetail detail : details) {
            BankVoucherTypeInferResultVO.Item item = new BankVoucherTypeInferResultVO.Item();
            item.setId(detail.getId());
            item.setTradeSerialNo(detail.getTradeSerialNo());
            item.setBookkeepingDate(detail.getBookkeepingDate());
            item.setPreviousType(detail.getVoucherType());

            if (detail.getWriteStatus() != null && detail.getWriteStatus() == 1) {
                item.setMatched(false);
                item.setMessage("已写入金蝶，跳过");
                result.setSkippedWritten(result.getSkippedWritten() + 1);
                result.getItems().add(item);
                continue;
            }

            BigDecimal debit = scale(detail.getDebitAmount());
            BigDecimal credit = scale(detail.getCreditAmount());
            boolean outflow = debit != null && debit.compareTo(BigDecimal.ZERO) > 0;
            boolean inflow = credit != null && credit.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal amount = outflow ? debit : (inflow ? credit : null);
            if (amount == null) {
                item.setMatched(false);
                item.setMessage("借贷金额均为空");
                result.setUnmatched(result.getUnmatched() + 1);
                result.getItems().add(item);
                continue;
            }
            item.setAmountSide(outflow ? "借方支出" : "贷方收入");
            item.setAmount(amount.toPlainString());

            String bankAccount;
            String bankDimension;
            try {
                bankAccount = channelService.resolveBankAccountForDetail(detail);
                bankDimension = channelService.resolveBankDimensionForDetail(detail);
            } catch (RuntimeException e) {
                item.setMatched(false);
                item.setMessage(e.getMessage());
                result.setUnmatched(result.getUnmatched() + 1);
                result.getItems().add(item);
                continue;
            }

            // 优先按「同科目+同维度+同日期+同金额+同方向」匹配；再放宽维度/科目/邻近年份
            List<String> dateKeys = candidateDates(detail);
            if (dateKeys.isEmpty()) {
                item.setMatched(false);
                item.setMessage("缺少记账日期，无法按日期金额匹配");
                result.setUnmatched(result.getUnmatched() + 1);
                result.getItems().add(item);
                continue;
            }
            String side = outflow ? "OUT" : "IN";
            BankHit best = takeBankHit(bankIndex, bankAccount, bankDimension, dateKeys, side, amount);

            if (best != null) {
                List<VoucherDetailVO> siblings = voucherGroups.getOrDefault(best.groupKey, Collections.emptyList());
                // 对方科目：排除凭证内任意 1002 银行分录
                VoucherDetailVO counterpart = pickCounterpart(siblings, best.entry.getFaccountID(), amount, outflow);
                if (counterpart != null) {
                    String type = BankVoucherTypeClassifier.inferFromKingdee(
                            counterpart.getFaccountID(), counterpart.getFexplanation(), counterpart.getFaccountName());
                    // 税费多借方时优先 TAX
                    if (countTaxLikeDebits(siblings, bankAccount) >= 2) {
                        type = BankVoucherTypeClassifier.TAX;
                    }
                    // 建行摘要明确手续费时，覆盖金蝶历史误记到 2202 货款的分录
                    if (BankVoucherTypeClassifier.isLocalFeeSignal(detail.getSummary(),
                            joinText(detail.getRemark(), detail.getPurpose()))
                            && BankVoucherTypeClassifier.canOverrideWithLocalFee(type)) {
                        type = BankVoucherTypeClassifier.localFeeType(detail.getSummary(),
                                joinText(detail.getRemark(), detail.getPurpose()));
                        applyInferredType(detail, item, result, type, counterpart.getFexplanation(),
                                counterpart.getFaccountID(), counterpart.getFaccountName(),
                                best.entry.getFbillTypeID(), best.entry.getFvoucherNo(), dryRun,
                                "本地摘要为手续费，覆盖金蝶对方科目 "
                                        + defaultText(counterpart.getFaccountID(), "")
                                        + "（原匹配 "
                                        + defaultText(best.entry.getFbillTypeID(), "")
                                        + "-" + defaultText(best.entry.getFvoucherNo(), "") + "）",
                                account, sessionId, channel);
                        continue;
                    }
                    applyInferredType(detail, item, result, type, counterpart.getFexplanation(),
                            counterpart.getFaccountID(), counterpart.getFaccountName(),
                            best.entry.getFbillTypeID(), best.entry.getFvoucherNo(), dryRun,
                            "已匹配金蝶同日期同金额凭证 "
                                    + defaultText(best.entry.getFbillTypeID(), "")
                                    + "-" + defaultText(best.entry.getFvoucherNo(), "")
                                    + "（" + defaultText(best.entry.getFaccountID(), bankAccount)
                                    + "/" + defaultText(best.entry.getFbankDimension(), bankDimension) + "）",
                            account, sessionId, channel);
                    continue;
                }
            }

            // 金蝶未命中时，再用本地摘要/备注/对方户名兜底
            // 业务：贷方且付款人(F列/对方户名)非本公司 → 对公收款或扫码收款
            String ownName = resolveOwnCompanyName(detail);
            String localType = BankVoucherTypeClassifier.classify(
                    detail.getSummary(),
                    joinText(detail.getRemark(), detail.getPurpose()),
                    detail.getDebitAmount(), detail.getCreditAmount(),
                    detail.getCounterpartyName(),
                    ownName);
            if (!BankVoucherTypeClassifier.OTHER.equals(localType)) {
                boolean qr = BankVoucherTypeClassifier.isQrReceiptSignal(
                        detail.getSummary(), detail.getRemark(), detail.getPurpose(),
                        detail.getCounterpartyName());
                String hint = "";
                if (BankVoucherTypeClassifier.RECEIPT.equals(localType)) {
                    hint = qr ? "（扫码收款）" : "（对公收款）";
                }
                applyInferredType(detail, item, result, localType, null, null, null, null, null, dryRun,
                        "金蝶未匹配，本地识别为 " + BankVoucherTypeClassifier.typeName(localType) + hint,
                        account, sessionId, channel);
                continue;
            }

            item.setMatched(false);
            item.setMessage(best == null
                    ? "金蝶未找到银行存款(1002)同日期同金额分录（已查 " + String.join(",", dateKeys)
                    + "）；本地也无法识别类型（贷方收款需对方户名非本公司，或摘要含 IBPS/HVPS/扫码清算）"
                    : "找到银行分录但无对方科目");
            if (best != null) {
                item.setMatchedVoucherWord(best.entry.getFbillTypeID());
                item.setMatchedVoucherNo(best.entry.getFvoucherNo());
            }
            result.setUnmatched(result.getUnmatched() + 1);
            result.getItems().add(item);
        }

        return result;
    }

    /**
     * 匹配顺序：渠道科目+维度 → 空维度 → 任意 1002 + 候选日期；同键多条一对一消耗。
     */
    private BankHit takeBankHit(Map<String, List<BankHit>> bankIndex,
                                String bankAccount, String bankDimension,
                                List<String> dateKeys, String side, BigDecimal amount) {
        for (String dateKey : dateKeys) {
            BankHit hit = takeFirst(bankIndex.get(
                    indexKey(bankAccount, bankDimension, dateKey, side, amount)));
            if (hit == null && StringUtils.hasText(bankDimension)) {
                hit = takeFirst(bankIndex.get(
                        indexKey(bankAccount, "", dateKey, side, amount)));
            }
            if (hit == null) {
                // 任意 1002.*（如渠道配 1002.09、金蝶实账 1002.01）
                for (Map.Entry<String, List<BankHit>> e : bankIndex.entrySet()) {
                    String key = e.getKey();
                    // key = account|dimension|date|side|amount
                    if (!key.contains("|" + dateKey + "|" + side + "|" + amount.toPlainString())) {
                        continue;
                    }
                    if (!key.startsWith("1002.")) {
                        continue;
                    }
                    hit = takeFirst(e.getValue());
                    if (hit != null) {
                        break;
                    }
                }
            }
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private BankHit takeFirst(List<BankHit> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.remove(0);
    }

    private String joinText(String a, String b) {
        if (!StringUtils.hasText(a)) {
            return b;
        }
        if (!StringUtils.hasText(b)) {
            return a;
        }
        return a.trim() + " " + b.trim();
    }

    private String resolveOwnCompanyName(KingdeeBankVoucherDetail detail) {
        if (detail != null && StringUtils.hasText(detail.getAccountName())) {
            return detail.getAccountName().trim();
        }
        if (detail != null && StringUtils.hasText(detail.getChannelKey())) {
            try {
                KingdeeBankChannel channel = channelService.requireByKey(detail.getChannelKey());
                if (channel != null && StringUtils.hasText(channel.getAccountName())) {
                    return channel.getAccountName().trim();
                }
            } catch (RuntimeException ignored) {
                // ignore
            }
        }
        return "广东汾源酒业有限公司";
    }

    private void applyInferredType(KingdeeBankVoucherDetail detail,
                                   BankVoucherTypeInferResultVO.Item item,
                                   BankVoucherTypeInferResultVO result,
                                   String type,
                                   String matchedExplanation,
                                   String matchedAccountCode,
                                   String matchedAccountName,
                                   String voucherWord,
                                   String voucherNo,
                                   boolean dryRun,
                                   String messagePrefix,
                                   KingdeeAccountContext account,
                                   String sessionId,
                                   KingdeeBankChannel channel) {
        item.setMatched(true);
        item.setVoucherType(type);
        item.setVoucherTypeName(BankVoucherTypeClassifier.typeName(type));
        boolean writable = BankVoucherTypeClassifier.isWritable(type);
        String writeHint;
        if (writable && BankVoucherTypeClassifier.requiresCounterparty(
                type,
                detail.getSummary(),
                joinText(detail.getRemark(), detail.getPurpose()),
                detail.getPurpose(),
                detail.getCounterpartyName())
                && !StringUtils.hasText(detail.getCounterpartyName())) {
            writable = false;
            writeHint = BankVoucherTypeClassifier.typeName(type) + "需要对方户名，当前为空，不可写入";
        } else if (writable) {
            // 与写入金蝶同一套档案校验：缺客户/员工/供应商时提前标不可写
            boolean needsMaster = BankVoucherTypeClassifier.RECEIPT.equals(type)
                    || BankVoucherTypeClassifier.EXPENSE.equals(type)
                    || BankVoucherTypeClassifier.GOODS.equals(type)
                    || BankVoucherTypeClassifier.FREIGHT.equals(type);
            String masterBlocker = needsMaster
                    ? resolveMasterDataBlocker(detail, type, account, sessionId, channel)
                    : null;
            if (StringUtils.hasText(masterBlocker)) {
                writable = false;
                writeHint = "档案缺失，不可写入：" + masterBlocker;
                result.setMasterDataBlocked(result.getMasterDataBlocked() + 1);
            } else {
                writeHint = needsMaster ? "可写入（档案齐全）" : "可写入";
            }
        } else {
            writeHint = BankVoucherTypeClassifier.nonWritableReason(type);
        }
        item.setWritable(writable);
        item.setMatchedExplanation(matchedExplanation);
        item.setMatchedAccountCode(matchedAccountCode);
        item.setMatchedAccountName(matchedAccountName);
        item.setMatchedVoucherWord(voucherWord);
        item.setMatchedVoucherNo(voucherNo);
        item.setMessage(messagePrefix + "，" + writeHint);

        if (BankVoucherTypeClassifier.OTHER.equals(type) && StringUtils.hasText(matchedAccountCode)) {
            result.getUnknownAccounts().putIfAbsent(matchedAccountCode,
                    defaultText(matchedAccountName, "")
                            + " / " + defaultText(matchedExplanation, ""));
        }

        result.setMatched(result.getMatched() + 1);
        if (item.isWritable()) {
            result.setWritableMatched(result.getWritableMatched() + 1);
        } else {
            result.setNonWritableMatched(result.getNonWritableMatched() + 1);
        }

        if (!dryRun) {
            // 反推只回填类型，不标记「金蝶已有」（是否重复由「检查金蝶重复」按同日期同金额判定）
            detail.setVoucherType(type);
            detail.setVoucherTypeName(BankVoucherTypeClassifier.typeName(type));
            detail.setUpdateTime(LocalDateTime.now());
            detailMapper.updateById(detail);
            result.setUpdated(result.getUpdated() + 1);
        }

        result.getItems().add(item);
    }

    /**
     * 按类型预检金蝶主数据；返回 null 表示可写，否则为与写入失败一致的可读原因。
     */
    private String resolveMasterDataBlocker(KingdeeBankVoucherDetail detail, String type,
                                            KingdeeAccountContext account, String sessionId,
                                            KingdeeBankChannel channel) {
        if (account == null || !StringUtils.hasText(sessionId)) {
            return null;
        }
        String useOrgCode = StringUtils.hasText(account.getUseOrgCode())
                ? account.getUseOrgCode().trim() : null;
        String orgNameHint = resolveCustomerOrgNameHint(account, channel);
        try {
            if (BankVoucherTypeClassifier.RECEIPT.equals(type)) {
                return resolveReceiptCustomerBlocker(detail, account, sessionId, useOrgCode, orgNameHint);
            }
            if (BankVoucherTypeClassifier.EXPENSE.equals(type)) {
                String name = trimToNull(detail.getCounterpartyName());
                if (name == null) {
                    return "支付费用需要对方户名（员工），当前为空";
                }
                String employeeNo = voucherQueryService.findEmployeeNumberByName(
                        account.getKingdeeUrl(), sessionId, name);
                if (!StringUtils.hasText(employeeNo)) {
                    return "未在金蝶员工档案找到「" + name + "」";
                }
                return null;
            }
            if (BankVoucherTypeClassifier.GOODS.equals(type)
                    || BankVoucherTypeClassifier.FREIGHT.equals(type)) {
                String name = trimToNull(detail.getCounterpartyName());
                if (name == null) {
                    return BankVoucherTypeClassifier.typeName(type) + "需要对方户名（供应商），当前为空";
                }
                if (PAYABLE_SUPPLIER_XINYANG_LICHENG.equals(name)) {
                    return null;
                }
                String supplierNo = supplierQueryService.findSupplierNumberByName(
                        account.getKingdeeUrl(), sessionId, name, useOrgCode);
                if (!StringUtils.hasText(supplierNo)) {
                    return supplierQueryService.describeMissingSupplier(
                            account.getKingdeeUrl(), sessionId, name, useOrgCode);
                }
                return null;
            }
        } catch (Exception e) {
            log.warn("反推档案预检异常 serial={} type={}：{}",
                    detail.getTradeSerialNo(), type, e.getMessage());
            return "档案预检异常：" + e.getMessage();
        }
        return null;
    }

    private String resolveReceiptCustomerBlocker(KingdeeBankVoucherDetail detail,
                                                 KingdeeAccountContext account, String sessionId,
                                                 String useOrgCode, String orgNameHint) {
        boolean qr = BankVoucherTypeClassifier.isQrReceiptSignal(
                detail.getSummary(), detail.getRemark(), detail.getPurpose(),
                detail.getCounterpartyName());
        String name = trimToNull(detail.getCounterpartyName());
        if (StringUtils.hasText(name)
                && !BankVoucherTypeClassifier.isQrSettlementCounterparty(name)
                && !BankVoucherTypeClassifier.isOwnCompanyName(name, detail.getAccountName())) {
            String no = voucherQueryService.findCustomerNumberByName(
                    account.getKingdeeUrl(), sessionId, name, useOrgCode, orgNameHint);
            if (StringUtils.hasText(no)) {
                return null;
            }
            if (!qr) {
                return voucherQueryService.describeMissingCustomer(
                        account.getKingdeeUrl(), sessionId, name, useOrgCode, orgNameHint);
            }
        }
        if (qr) {
            for (String fallback : qrCustomerNameCandidates()) {
                String no = voucherQueryService.findCustomerNumberByName(
                        account.getKingdeeUrl(), sessionId, fallback, useOrgCode, orgNameHint);
                if (StringUtils.hasText(no)) {
                    return null;
                }
            }
            String lookup = defaultText(properties.getQrCustomerName(), "扫码收款");
            return voucherQueryService.describeMissingCustomer(
                    account.getKingdeeUrl(), sessionId, lookup, useOrgCode, orgNameHint);
        }
        if (!StringUtils.hasText(name)) {
            return "收到货款需要对方户名（客户），当前为空";
        }
        return voucherQueryService.describeMissingCustomer(
                account.getKingdeeUrl(), sessionId, name, useOrgCode, orgNameHint);
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

    private String resolveCustomerOrgNameHint(KingdeeAccountContext account, KingdeeBankChannel channel) {
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

    private Map<String, List<VoucherDetailVO>> groupByVoucher(List<VoucherDetailVO> entries) {
        Map<String, List<VoucherDetailVO>> map = new HashMap<>();
        for (VoucherDetailVO e : entries) {
            map.computeIfAbsent(voucherKey(e), k -> new ArrayList<>()).add(e);
        }
        return map;
    }

    private Map<String, List<BankHit>> buildBankIndex(List<VoucherDetailVO> entries) {
        Map<String, List<BankHit>> index = new HashMap<>();
        for (VoucherDetailVO e : entries) {
            if (!StringUtils.hasText(e.getFaccountID())) {
                continue;
            }
            String account = e.getFaccountID().trim();
            if (!account.startsWith("1002.")) {
                continue;
            }
            String dimension = StringUtils.hasText(e.getFbankDimension())
                    ? e.getFbankDimension().trim() : "";
            String dateKey = normalizeDateDigits(e.getFdate());
            if (!StringUtils.hasText(dateKey)) {
                continue;
            }
            BigDecimal debit = scale(e.getFdebit());
            BigDecimal credit = scale(e.getFcredit());
            // 银行借方 = 流水贷方收入；银行贷方 = 流水借方支出
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                BankHit hit = new BankHit(e, voucherKey(e));
                index.computeIfAbsent(
                        indexKey(account, dimension, dateKey, "IN", debit),
                        k -> new ArrayList<>()).add(hit);
            }
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                BankHit hit = new BankHit(e, voucherKey(e));
                index.computeIfAbsent(
                        indexKey(account, dimension, dateKey, "OUT", credit),
                        k -> new ArrayList<>()).add(hit);
            }
        }
        return index;
    }

    private String indexKey(String bankAccount, String bankDimension, String dateKey,
                            String side, BigDecimal amount) {
        return bankAccount + "|" + defaultText(bankDimension, "") + "|"
                + dateKey + "|" + side + "|" + amount.toPlainString();
    }

    /** 与重复检查一致：优先记账日期，并兼容邻近年份同月日 */
    private String extractDetailDate(KingdeeBankVoucherDetail detail) {
        String book = normalizeDateDigits(detail.getBookkeepingDate());
        if (StringUtils.hasText(book) && book.length() >= 8) {
            return book.substring(0, 8);
        }
        if (StringUtils.hasText(detail.getTradeTime())) {
            String digits = detail.getTradeTime().replaceAll("\\D", "");
            if (digits.length() >= 8) {
                return digits.substring(0, 8);
            }
        }
        return book;
    }

    private List<String> candidateDates(KingdeeBankVoucherDetail detail) {
        LinkedHashSet<String> dates = new LinkedHashSet<>();
        String primary = extractDetailDate(detail);
        if (StringUtils.hasText(primary) && primary.length() >= 8) {
            dates.add(primary.substring(0, 8));
            String md = primary.substring(4, 8);
            try {
                int year = Integer.parseInt(primary.substring(0, 4));
                for (int delta = -2; delta <= 2; delta++) {
                    if (delta == 0) {
                        continue;
                    }
                    dates.add((year + delta) + md);
                }
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        return new ArrayList<>(dates);
    }

    private VoucherDetailVO pickCounterpart(List<VoucherDetailVO> siblings, String bankAccount,
                                            BigDecimal amount, boolean outflow) {
        if (siblings == null || siblings.isEmpty()) {
            return null;
        }
        List<VoucherDetailVO> others = siblings.stream()
                .filter(e -> !isAnyBankAccount(e.getFaccountID()))
                .collect(Collectors.toList());
        if (others.isEmpty()) {
            return null;
        }
        // 支出：对方一般在借方；收入：对方一般在贷方。同金额时优先手续费科目，避免误取货款分录
        List<VoucherDetailVO> amountMatched = new ArrayList<>();
        for (VoucherDetailVO e : others) {
            BigDecimal side = outflow ? scale(e.getFdebit()) : scale(e.getFcredit());
            if (side != null && side.compareTo(amount) == 0) {
                amountMatched.add(e);
            }
        }
        if (!amountMatched.isEmpty()) {
            for (VoucherDetailVO e : amountMatched) {
                if (isFeeAccount(e.getFaccountID(), e.getFaccountName(), e.getFexplanation())) {
                    return e;
                }
            }
            return amountMatched.get(0);
        }
        // 多科目拆分时取金额最大的非银行分录
        VoucherDetailVO best = null;
        BigDecimal bestAmt = BigDecimal.ZERO;
        for (VoucherDetailVO e : others) {
            BigDecimal d = scale(e.getFdebit());
            BigDecimal c = scale(e.getFcredit());
            BigDecimal a = BigDecimal.ZERO;
            if (d != null) {
                a = a.add(d);
            }
            if (c != null) {
                a = a.add(c);
            }
            if (a.compareTo(bestAmt) > 0) {
                bestAmt = a;
                best = e;
            }
        }
        return best != null ? best : others.get(0);
    }

    private boolean isFeeAccount(String accountCode, String accountName, String explanation) {
        String code = accountCode == null ? "" : accountCode.trim();
        String name = accountName == null ? "" : accountName;
        String exp = explanation == null ? "" : explanation;
        return "6603.04".equals(code)
                || (name.contains("财务费用") && name.contains("手续费"))
                || exp.contains("手续费")
                || exp.contains("收费");
    }

    private int countTaxLikeDebits(List<VoucherDetailVO> siblings, String bankAccount) {
        int count = 0;
        for (VoucherDetailVO e : siblings) {
            if (isAnyBankAccount(e.getFaccountID())) {
                continue;
            }
            String code = e.getFaccountID() == null ? "" : e.getFaccountID();
            BigDecimal debit = scale(e.getFdebit());
            if (code.startsWith("2221") && debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                count++;
            }
        }
        return count;
    }

    private boolean isBankAccount(String accountCode, String bankAccount) {
        if (!StringUtils.hasText(accountCode) || !StringUtils.hasText(bankAccount)) {
            return false;
        }
        return accountCode.trim().equals(bankAccount.trim());
    }

    /** 任意银行存款明细科目（1002.*） */
    private boolean isAnyBankAccount(String accountCode) {
        return StringUtils.hasText(accountCode) && accountCode.trim().startsWith("1002.");
    }

    private String voucherKey(VoucherDetailVO e) {
        return String.join("|",
                Objects.toString(e.getFyear(), ""),
                Objects.toString(e.getFperiod(), ""),
                Objects.toString(e.getFbillTypeID(), ""),
                Objects.toString(e.getFvoucherNo(), ""),
                Objects.toString(normalizeDateDigits(e.getFdate()), ""));
    }

    private String toAccountingPeriod(String bookkeepingDate) {
        String digits = normalizeDateDigits(bookkeepingDate);
        if (digits == null || digits.length() < 6) {
            return null;
        }
        return digits.substring(0, 6);
    }

    private String normalizeDateDigits(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 8);
        }
        if (digits.length() == 6) {
            return digits + "01";
        }
        return digits.isEmpty() ? null : digits;
    }

    private String shiftPeriod(String yyyymm, int deltaMonths) {
        int year = Integer.parseInt(yyyymm.substring(0, 4));
        int month = Integer.parseInt(yyyymm.substring(4, 6));
        month += deltaMonths;
        while (month <= 0) {
            month += 12;
            year--;
        }
        while (month > 12) {
            month -= 12;
            year++;
        }
        return year + String.format("%02d", month);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static class BankHit {
        private final VoucherDetailVO entry;
        private final String groupKey;

        private BankHit(VoucherDetailVO entry, String groupKey) {
            this.entry = entry;
            this.groupKey = groupKey;
        }
    }
}
