package com.fenyuan.liquor.modules.kingdee.service;
import com.fenyuan.liquor.modules.kingdee.config.KingdeeBankVoucherProperties;
import com.fenyuan.liquor.modules.kingdee.dto.KingdeeAccountContext;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankVoucherDetail;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeBankVoucherDetailMapper;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherFeeAmounts;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherTypeClassifier;
import com.fenyuan.liquor.modules.kingdee.vo.BankVoucherKingdeeExistResultVO;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 按「渠道科目 + 核算维度 + 日期 + 金额」检查金蝶银行分录是否已存在。
 * 命中后回写银字号（凭证字+凭证号），避免重复写入。
 * 同键多条时一对一消耗，避免一条金蝶分录把多笔本地明细都标成「已存在」。
 * 查重刻意收紧：不以「任意 1002 同日同额」命中；跨年仅在同科目+同维度时放行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankVoucherKingdeeExistService {
    private final KingdeeBankVoucherDetailService detailService;
    private final KingdeeBankVoucherDetailMapper detailMapper;
    private final KingdeeAccountResolverService accountResolverService;
    private final KingdeeLoginService loginService;
    private final KingdeeVoucherQueryService voucherQueryService;
    private final KingdeeBankVoucherProperties properties;
    private final KingdeeBankChannelService channelService;
    @Transactional
    public BankVoucherKingdeeExistResultVO checkExists(List<Long> ids, Long accountId,
                                                       List<String> accountingPeriods) {
        return checkExists(ids, accountId, null, accountingPeriods);
    }

    @Transactional
    public BankVoucherKingdeeExistResultVO checkExists(List<Long> ids, Long accountId, String channelKey,
                                                       List<String> accountingPeriods) {
        List<KingdeeBankVoucherDetail> details;
        if (ids != null && !ids.isEmpty()) {
            details = detailService.findByIds(ids);
        } else {
            details = detailService.search(channelKey, null, null, null, null, null, null);
        }
        if (details.isEmpty()) {
            throw new RuntimeException("没有可检查的银行明细");
        }
        KingdeeBankChannel channel = channelService.resolveForOperation(channelKey, details);
        KingdeeAccountContext account = accountResolverService.resolveForOperation(accountId);
        String orgCompanyCode = channelService.resolveOrgCompanyCode(channel, account);
        String formId = StringUtils.hasText(properties.getFormId())
                ? properties.getFormId() : "GL_VOUCHER";
        Set<String> periods = collectPeriods(details, accountingPeriods);
        final String sessionId;
        try {
            sessionId = loginService.login(
                    account.getKingdeeUrl(), account.getDbId(), account.getUsername(), account.getPassword());
        } catch (java.io.IOException e) {
            throw new RuntimeException("登录金蝶失败：" + e.getMessage(), e);
        }
        List<VoucherDetailVO> allEntries = queryKingdeeEntries(account, sessionId, formId, orgCompanyCode, periods);
        // 索引键：科目|维度|日期|方向|金额
        Map<String, List<VoucherDetailVO>> bankIndex = buildBankIndex(allEntries);
        // 宽松索引：科目|日期|方向|金额（忽略维度，兼容历史凭证未填银行核算维度）
        Map<String, List<VoucherDetailVO>> looseIndex = buildLooseBankIndex(allEntries);
        // 注意：不再用「任意 1002 + 同日同额」做查重命中，避免租金等同额流水误判为已存在
        int yinCount = 0;
        int bank1002Count = 0;
        for (VoucherDetailVO e : allEntries) {
            if (StringUtils.hasText(e.getFaccountID()) && e.getFaccountID().trim().startsWith("1002.")) {
                bank1002Count++;
            }
            if (isYinVoucher(e)) {
                yinCount++;
            }
        }
        log.info("银行流水查重：期间={} 金蝶分录={} 银字分录={} 银行1002分录={} 渠道科目={}",
                periods, allEntries.size(), yinCount, bank1002Count,
                channelService.resolveBankAccount(channel));
        BankVoucherKingdeeExistResultVO result = new BankVoucherKingdeeExistResultVO();
        result.setTotal(details.size());
        result.setPeriodsQueried(new ArrayList<>(periods));
        result.setKingdeeEntryCount(allEntries.size());
        if (allEntries.isEmpty()) {
            log.warn("银行流水查重期间内金蝶分录为空 periods={} org={}，请核对流水年份与账套组织",
                    periods, orgCompanyCode);
        }
        for (KingdeeBankVoucherDetail detail : details) {
            BankVoucherKingdeeExistResultVO.Item item = new BankVoucherKingdeeExistResultVO.Item();
            item.setId(detail.getId());
            item.setTradeSerialNo(detail.getTradeSerialNo());
            item.setTradeTime(detail.getTradeTime());
            item.setBookkeepingDate(detail.getBookkeepingDate());
            BigDecimal debit = scale(detail.getDebitAmount());
            BigDecimal credit = scale(detail.getCreditAmount());
            boolean outflow = debit != null && debit.compareTo(BigDecimal.ZERO) > 0;
            boolean inflow = credit != null && credit.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal amount = outflow ? debit : (inflow ? credit : null);
            List<String> primaryDateKeys = resolvePrimaryDates(detail);
            List<String> yearExpandedDateKeys = resolveYearExpandedDates(primaryDateKeys);
            if (amount == null || primaryDateKeys.isEmpty()) {
                item.setKingdeeExistStatus(0);
                item.setMessage("缺少日期或金额，无法检查");
                result.setUncheckedCount(result.getUncheckedCount() + 1);
                result.getItems().add(item);
                continue;
            }
            KingdeeBankChannel detailChannel;
            String bankAccount;
            String bankDimension;
            try {
                detailChannel = channelService.resolveForDetail(detail);
                bankAccount = channelService.resolveBankAccount(detailChannel);
                bankDimension = channelService.resolveBankDimension(detailChannel);
            } catch (RuntimeException e) {
                item.setKingdeeExistStatus(0);
                item.setMessage(e.getMessage());
                result.setUncheckedCount(result.getUncheckedCount() + 1);
                result.getItems().add(item);
                continue;
            }
            item.setAmountSide(outflow ? "借方支出" : "贷方收入");
            item.setAmount(amount.toPlainString());
            String side = outflow ? "OUT" : "IN";
            boolean qr = inflow && BankVoucherTypeClassifier.isQrReceiptSignal(
                    detail.getSummary(), detail.getRemark(), detail.getPurpose(),
                    detail.getCounterpartyName());
            BigDecimal feeRate = channelService.isQrFeeExempt(detailChannel)
                    ? BigDecimal.ZERO
                    : parseQrFeeRateSafe();
            List<BigDecimal> amountCandidates = BankVoucherFeeAmounts.candidateMatchAmounts(
                    amount, qr,
                    detail.getSummary(), detail.getRemark(), detail.getPurpose(),
                    feeRate);
            // 扫码：优先匹配银行到账净额，再匹配「到账+摘要FEE」客户实付额
            VoucherDetailVO hit = null;
            BigDecimal matchedAmount = amount;
            for (BigDecimal candidate : amountCandidates) {
                hit = consumeHit(bankIndex, looseIndex,
                        bankAccount, bankDimension, primaryDateKeys, yearExpandedDateKeys, side, candidate);
                if (hit != null) {
                    matchedAmount = candidate;
                    break;
                }
            }
            // 扫码未命中 1002 时：用「到账+FEE」去匹配应收/收入等同日期金额（历史可能按总额记账）
            if (hit == null && qr && amountCandidates.size() > 1) {
                BigDecimal gross = amountCandidates.get(1);
                hit = findNonBankSameAmount(allEntries, primaryDateKeys, gross);
                if (hit != null) {
                    matchedAmount = gross;
                }
            }
            // 独立手续费流水：优先 1002 贷方；未命中时再按 6603 借方等额匹配（各自一张银字）
            boolean feeLine = outflow && (BankVoucherTypeClassifier.isBankRemittanceFee(
                    detail.getSummary(), joinText(detail.getRemark(), detail.getPurpose()))
                    || BankVoucherTypeClassifier.isLocalFeeSignal(
                    detail.getSummary(), joinText(detail.getRemark(), detail.getPurpose())));
            if (hit == null && feeLine) {
                hit = findFeeAccountHit(allEntries, primaryDateKeys, amount, true);
                if (hit != null) {
                    matchedAmount = amount;
                }
            }
            if (hit != null) {
                String word = defaultText(hit.getFbillTypeID(), properties.getVoucherGroup());
                String no = defaultText(hit.getFvoucherNo(), "");
                String hitDate = defaultText(normalizeDateDigits(hit.getFdate()), "");
                BigDecimal fee = BankVoucherFeeAmounts.parseFeeFromText(
                        detail.getSummary(), detail.getRemark(), detail.getPurpose());
                String feeHint = (qr && fee != null)
                        ? "，摘要FEE=" + fee.toPlainString()
                        + "，核对金额=" + matchedAmount.toPlainString()
                        : (feeLine ? "，手续费科目" : "");
                String crossYearHint = "";
                if (StringUtils.hasText(hitDate) && !primaryDateKeys.contains(hitDate)) {
                    crossYearHint = "，跨年日期匹配流水日 "
                            + String.join("/", primaryDateKeys) + "↔金蝶" + hitDate;
                }
                item.setKingdeeExistStatus(1);
                item.setKingdeeExistVoucherWord(word);
                item.setKingdeeExistVoucherNo(no);
                item.setAmount(matchedAmount.toPlainString());
                item.setMessage("金蝶已存在 "
                        + word + "-" + no
                        + "（" + defaultText(hit.getFaccountID(), bankAccount)
                        + "/" + defaultText(hit.getFbankDimension(), defaultText(bankDimension, "-"))
                        + " 日期" + defaultText(hitDate, "-")
                        + " 同金额" + feeHint + crossYearHint + "）");
                detail.setKingdeeExistStatus(1);
                detail.setKingdeeExistVoucherWord(word);
                detail.setKingdeeExistVoucherNo(no);
                detail.setKingdeeExistCheckedAt(LocalDateTime.now());
                // 查重命中视为已写入：前端显示凭证号且不可再勾选
                markExistAsWritten(detail, word, no);
                detail.setUpdateTime(LocalDateTime.now());
                detailMapper.updateById(detail);
                result.setExistsCount(result.getExistsCount() + 1);
            } else {
                BigDecimal fee = BankVoucherFeeAmounts.parseFeeFromText(
                        detail.getSummary(), detail.getRemark(), detail.getPurpose());
                String feeHint = (qr && fee != null)
                        ? "；扫码摘要FEE=" + fee.toPlainString()
                        + "，应核对到账 " + amount.toPlainString()
                        + " 或实付 " + amount.add(fee).toPlainString()
                        : "";
                String nearMiss = findNearMissMessage(allEntries, primaryDateKeys, amount);
                if (!StringUtils.hasText(nearMiss) && amountCandidates.size() > 1) {
                    nearMiss = findNearMissMessage(allEntries, primaryDateKeys, amountCandidates.get(1));
                }
                item.setKingdeeExistStatus(2);
                item.setMessage("金蝶未找到银行存款(1002)同日期金额分录（已查 "
                        + String.join(",", primaryDateKeys) + "）"
                        + feeHint
                        + (StringUtils.hasText(nearMiss) ? "；" + nearMiss : ""));
                detail.setKingdeeExistStatus(2);
                detail.setKingdeeExistVoucherWord(null);
                detail.setKingdeeExistVoucherNo(null);
                detail.setKingdeeExistCheckedAt(LocalDateTime.now());
                // 金蝶无：清空平台写入标记，前端可重新勾选写入
                clearPlatformWriteMark(detail);
                detail.setUpdateTime(LocalDateTime.now());
                detailMapper.updateById(detail);
                result.setNotExistsCount(result.getNotExistsCount() + 1);
            }
            result.getItems().add(item);
        }
        return result;
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

    /** 手续费科目借方等同金额（独立手续费银字） */
    private VoucherDetailVO findFeeAccountHit(List<VoucherDetailVO> entries, List<String> dateKeys,
                                              BigDecimal amount, boolean preferYin) {
        if (entries == null || dateKeys == null || amount == null) {
            return null;
        }
        String feeAccount = StringUtils.hasText(properties.getFeeAccount())
                ? properties.getFeeAccount().trim() : "6603.04";
        VoucherDetailVO fallback = null;
        for (VoucherDetailVO e : entries) {
            if (!StringUtils.hasText(e.getFaccountID())) {
                continue;
            }
            String account = e.getFaccountID().trim();
            if (!account.equals(feeAccount) && !account.startsWith("6603.")) {
                continue;
            }
            String dateKey = normalizeDateDigits(e.getFdate());
            if (!dateKeys.contains(dateKey)) {
                continue;
            }
            BigDecimal debit = scale(e.getFdebit());
            if (debit == null || debit.compareTo(amount) != 0) {
                continue;
            }
            if (preferYin && isYinVoucher(e)) {
                return e;
            }
            if (fallback == null) {
                fallback = e;
            }
        }
        return fallback;
    }

    /**
     * 按候选日期依次匹配：
     * 1) 流水真实日期：精确维度 → 空维度 → 同科目忽略维度（宽松仅认银字）
     * 2) 邻近年份同月日：仅「同科目+同维度」精确命中（防止租金等同额跨年误判）
     * 命中后从各索引同时移除，保证一对一消耗。
     */
    private VoucherDetailVO consumeHit(Map<String, List<VoucherDetailVO>> bankIndex,
                                       Map<String, List<VoucherDetailVO>> looseIndex,
                                       String bankAccount, String bankDimension,
                                       List<String> primaryDates, List<String> yearExpandedDates,
                                       String side, BigDecimal amount) {
        for (String dateKey : primaryDates) {
            VoucherDetailVO hit = takePreferred(bankIndex.get(
                    indexKey(bankAccount, bankDimension, dateKey, side, amount)), false);
            if (hit == null && StringUtils.hasText(bankDimension)) {
                hit = takePreferred(bankIndex.get(
                        indexKey(bankAccount, "", dateKey, side, amount)), true);
            }
            if (hit == null) {
                // 忽略维度时必须是银字，避免转字/其他凭证同额误标已存在
                hit = takePreferred(looseIndex.get(
                        looseKey(bankAccount, dateKey, side, amount)), true);
            }
            if (hit != null) {
                removeFromAllIndexes(bankIndex, looseIndex, hit);
                return hit;
            }
        }
        // 跨年仅在科目+维度都齐备时放行，避免「任意银行同日同额」误命中
        if (StringUtils.hasText(bankAccount) && StringUtils.hasText(bankDimension)
                && yearExpandedDates != null) {
            for (String dateKey : yearExpandedDates) {
                VoucherDetailVO hit = takePreferred(bankIndex.get(
                        indexKey(bankAccount, bankDimension, dateKey, side, amount)), true);
                if (hit != null) {
                    removeFromAllIndexes(bankIndex, looseIndex, hit);
                    return hit;
                }
            }
        }
        return null;
    }

    private void removeFromAllIndexes(Map<String, List<VoucherDetailVO>> bankIndex,
                                      Map<String, List<VoucherDetailVO>> looseIndex,
                                      VoucherDetailVO hit) {
        for (List<VoucherDetailVO> list : bankIndex.values()) {
            list.removeIf(e -> e == hit);
        }
        for (List<VoucherDetailVO> list : looseIndex.values()) {
            list.removeIf(e -> e == hit);
        }
    }

    /**
     * @param requireYin true 时只取银字凭证，避免非银字同额分录误判
     */
    private VoucherDetailVO takePreferred(List<VoucherDetailVO> hits, boolean requireYin) {
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        for (int i = 0; i < hits.size(); i++) {
            if (isYinVoucher(hits.get(i))) {
                return hits.remove(i);
            }
        }
        if (requireYin) {
            return null;
        }
        return hits.remove(0);
    }

    private boolean isYinVoucher(VoucherDetailVO e) {
        if (e == null) {
            return false;
        }
        String word = defaultText(e.getFbillTypeID(), "");
        String expected = defaultText(properties.getVoucherGroup(), "银");
        return word.contains(expected) || "银".equals(word);
    }

    private Set<String> collectPeriods(List<KingdeeBankVoucherDetail> details, List<String> accountingPeriods) {
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
            List<String> primary = resolvePrimaryDates(d);
            for (String dateKey : primary) {
                String period = toAccountingPeriod(dateKey);
                if (period != null) {
                    periods.add(period);
                    periods.add(shiftPeriod(period, -1));
                    periods.add(shiftPeriod(period, 1));
                }
            }
            // 跨年精确匹配仍需拉邻近年同月账期
            for (String dateKey : resolveYearExpandedDates(primary)) {
                String period = toAccountingPeriod(dateKey);
                if (period != null) {
                    periods.add(period);
                }
            }
        }
        if (periods.isEmpty()) {
            throw new RuntimeException("明细缺少记账日期，无法确定会计期间");
        }
        return periods;
    }
    private List<VoucherDetailVO> queryKingdeeEntries(KingdeeAccountContext account, String sessionId,
                                                      String formId, String orgCompanyCode,
                                                      Set<String> periods) {
        List<VoucherDetailVO> allEntries = new ArrayList<>();
        for (String period : periods) {
            try {
                List<VoucherDetailVO> rows = voucherQueryService.queryVoucherDetails(
                        account.getKingdeeUrl(), sessionId, formId, period,
                        null, null, orgCompanyCode, KingdeeVoucherQueryService.ORG_FIELD_KEY,
                        true);
                if (rows != null) {
                    allEntries.addAll(rows);
                }
            } catch (Exception e) {
                log.warn("查询金蝶凭证失败 period={}：{}", period, e.getMessage());
            }
        }
        return allEntries;
    }
    private Map<String, List<VoucherDetailVO>> buildBankIndex(List<VoucherDetailVO> entries) {
        Map<String, List<VoucherDetailVO>> index = new HashMap<>();
        for (VoucherDetailVO e : entries) {
            if (!StringUtils.hasText(e.getFaccountID())) {
                continue;
            }
            String account = e.getFaccountID().trim();
            // 仅索引银行存款明细科目，避免无关分录干扰
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
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                index.computeIfAbsent(
                        indexKey(account, dimension, dateKey, "IN", debit),
                        k -> new ArrayList<>()).add(e);
            }
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                index.computeIfAbsent(
                        indexKey(account, dimension, dateKey, "OUT", credit),
                        k -> new ArrayList<>()).add(e);
            }
        }
        return index;
    }

    private Map<String, List<VoucherDetailVO>> buildLooseBankIndex(List<VoucherDetailVO> entries) {
        Map<String, List<VoucherDetailVO>> index = new HashMap<>();
        for (VoucherDetailVO e : entries) {
            if (!StringUtils.hasText(e.getFaccountID())) {
                continue;
            }
            String account = e.getFaccountID().trim();
            if (!account.startsWith("1002.")) {
                continue;
            }
            String dateKey = normalizeDateDigits(e.getFdate());
            if (!StringUtils.hasText(dateKey)) {
                continue;
            }
            BigDecimal debit = scale(e.getFdebit());
            BigDecimal credit = scale(e.getFcredit());
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                index.computeIfAbsent(
                        looseKey(account, dateKey, "IN", debit),
                        k -> new ArrayList<>()).add(e);
            }
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                index.computeIfAbsent(
                        looseKey(account, dateKey, "OUT", credit),
                        k -> new ArrayList<>()).add(e);
            }
        }
        return index;
    }

    private String indexKey(String bankAccount, String bankDimension, String dateKey,
                            String side, BigDecimal amount) {
        return bankAccount + "|" + defaultText(bankDimension, "") + "|"
                + dateKey + "|" + side + "|" + amount.toPlainString();
    }

    private String looseKey(String bankAccount, String dateKey, String side, BigDecimal amount) {
        return bankAccount + "|" + dateKey + "|" + side + "|" + amount.toPlainString();
    }

    /**
     * 同日期同金额但落在非银行存款科目时给出提示（常见：转字确认收入 1122/6001，不能当作银字已写入）。
     */
    private String findNearMissMessage(List<VoucherDetailVO> entries, List<String> dateKeys, BigDecimal amount) {
        VoucherDetailVO e = findNonBankSameAmount(entries, dateKeys, amount);
        if (e == null) {
            return null;
        }
        String word = defaultText(e.getFbillTypeID(), "");
        String no = defaultText(e.getFvoucherNo(), "");
        String account = e.getFaccountID() != null ? e.getFaccountID().trim() : "";
        return "发现同日期同金额但非银行存款科目："
                + word + "-" + no + "/" + account
                + "（多为转字收入确认等，不是银行流水银字，银行收款仍需单独写入）";
    }

    /** 非 1002 科目上同日期同金额分录（用于扫码按「到账+FEE」总额回查应收等） */
    private VoucherDetailVO findNonBankSameAmount(List<VoucherDetailVO> entries,
                                                  List<String> dateKeys, BigDecimal amount) {
        if (entries == null || dateKeys == null || amount == null) {
            return null;
        }
        VoucherDetailVO preferred = null;
        for (VoucherDetailVO e : entries) {
            if (!StringUtils.hasText(e.getFaccountID())) {
                continue;
            }
            String account = e.getFaccountID().trim();
            if (account.startsWith("1002.")) {
                continue;
            }
            String dateKey = normalizeDateDigits(e.getFdate());
            if (!dateKeys.contains(dateKey)) {
                continue;
            }
            BigDecimal debit = scale(e.getFdebit());
            BigDecimal credit = scale(e.getFcredit());
            boolean amountHit = (debit != null && debit.compareTo(amount) == 0)
                    || (credit != null && credit.compareTo(amount) == 0);
            if (!amountHit) {
                continue;
            }
            if (isYinVoucher(e)) {
                return e;
            }
            if (preferred == null) {
                preferred = e;
            }
        }
        return preferred;
    }

    private BigDecimal parseQrFeeRateSafe() {
        try {
            if (StringUtils.hasText(properties.getQrFeeRate())) {
                return new BigDecimal(properties.getQrFeeRate().trim());
            }
        } catch (Exception ignored) {
            // fall through
        }
        return new BigDecimal("0.003");
    }

    /**
     * 查重主日期：仅记账日与交易日（不扩年），避免同额租金跨年误判。
     */
    private List<String> resolvePrimaryDates(KingdeeBankVoucherDetail detail) {
        LinkedHashSet<String> dates = new LinkedHashSet<>();
        String book = normalizeDateDigits(detail.getBookkeepingDate());
        if (StringUtils.hasText(book) && book.length() >= 8) {
            dates.add(book.substring(0, 8));
        }
        if (StringUtils.hasText(detail.getTradeTime())) {
            String digits = detail.getTradeTime().replaceAll("\\D", "");
            if (digits.length() >= 8) {
                dates.add(digits.substring(0, 8));
            }
        }
        return new ArrayList<>(dates);
    }

    /**
     * 邻近年份同月日：仅用于「同科目+同维度」精确查重，兼容流水年份与账期年份偏差。
     */
    private List<String> resolveYearExpandedDates(List<String> primaryDates) {
        LinkedHashSet<String> dates = new LinkedHashSet<>();
        if (primaryDates == null) {
            return new ArrayList<>();
        }
        for (String d : primaryDates) {
            if (!StringUtils.hasText(d) || d.length() < 8) {
                continue;
            }
            String md = d.substring(4);
            int year;
            try {
                year = Integer.parseInt(d.substring(0, 4));
            } catch (NumberFormatException e) {
                continue;
            }
            for (int delta = -2; delta <= 2; delta++) {
                if (delta == 0) {
                    continue;
                }
                dates.add((year + delta) + md);
            }
        }
        return new ArrayList<>(dates);
    }

    private String toAccountingPeriod(String dateDigits) {
        if (!StringUtils.hasText(dateDigits) || dateDigits.length() < 6) {
            return null;
        }
        return dateDigits.substring(0, 6);
    }
    private String normalizeDateDigits(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 8);
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

    /**
     * 查重命中后同步为「已写入」，写入状态展示银字号，前端禁止再勾选。
     * 若本平台曾真实写入（非 EXIST- 占位），保留原凭证 ID。
     */
    private void markExistAsWritten(KingdeeBankVoucherDetail detail, String word, String no) {
        detail.setWriteStatus(1);
        detail.setKingdeeVoucherWord(defaultText(word, properties.getVoucherGroup()));
        detail.setKingdeeVoucherNo(no);
        detail.setWriteError(null);
        String existingId = detail.getKingdeeVoucherId();
        if (!StringUtils.hasText(existingId) || existingId.startsWith("EXIST-")) {
            detail.setKingdeeVoucherId("EXIST-"
                    + defaultText(word, properties.getVoucherGroup()) + "-" + defaultText(no, ""));
        }
        if (detail.getPlatformWriteTime() == null) {
            detail.setPlatformWriteTime(LocalDateTime.now());
        }
    }

    private void clearPlatformWriteMark(KingdeeBankVoucherDetail detail) {
        detail.setWriteStatus(0);
        detail.setKingdeeVoucherId(null);
        detail.setKingdeeVoucherNo(null);
        detail.setKingdeeVoucherWord(null);
        detail.setPlatformWriteTime(null);
        detail.setWriteError(null);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
