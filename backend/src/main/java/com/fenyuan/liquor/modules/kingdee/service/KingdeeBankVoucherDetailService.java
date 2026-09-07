package com.fenyuan.liquor.modules.kingdee.service;

import com.alibaba.excel.EasyExcel;
import com.fenyuan.liquor.modules.kingdee.dto.excel.KingdeeBankVoucherDetailExcel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankVoucherDetail;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeBankVoucherDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherTypeClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KingdeeBankVoucherDetailService {

    private final KingdeeBankVoucherDetailMapper mapper;
    private final KingdeeBankChannelService channelService;

    public List<KingdeeBankVoucherDetail> search(String channelKey,
                                                 String bookkeepingDateStart,
                                                 String bookkeepingDateEnd,
                                                 String voucherType,
                                                 Integer writeStatus,
                                                 Boolean writable,
                                                 String counterpartyName,
                                                 String summary,
                                                 String remark) {
        LambdaQueryWrapper<KingdeeBankVoucherDetail> q = new LambdaQueryWrapper<>();
        q.eq(KingdeeBankVoucherDetail::getDeleted, 0);
        if (StringUtils.hasText(channelKey)) {
            q.eq(KingdeeBankVoucherDetail::getChannelKey, channelKey.trim());
        }
        if (StringUtils.hasText(bookkeepingDateStart)) {
            q.ge(KingdeeBankVoucherDetail::getBookkeepingDate, bookkeepingDateStart.trim());
        }
        if (StringUtils.hasText(bookkeepingDateEnd)) {
            q.le(KingdeeBankVoucherDetail::getBookkeepingDate, bookkeepingDateEnd.trim());
        }
        if (StringUtils.hasText(voucherType)) {
            q.eq(KingdeeBankVoucherDetail::getVoucherType, voucherType.trim());
        }
        if (writeStatus != null) {
            q.eq(KingdeeBankVoucherDetail::getWriteStatus, writeStatus);
        }
        if (writable != null) {
            List<String> writableCodes = new ArrayList<>(BankVoucherTypeClassifier.writableTypes());
            if (writable) {
                q.in(KingdeeBankVoucherDetail::getVoucherType, writableCodes);
            } else {
                q.and(w -> w.isNull(KingdeeBankVoucherDetail::getVoucherType)
                        .or().eq(KingdeeBankVoucherDetail::getVoucherType, "")
                        .or().notIn(KingdeeBankVoucherDetail::getVoucherType, writableCodes));
            }
        }
        if (StringUtils.hasText(counterpartyName)) {
            q.like(KingdeeBankVoucherDetail::getCounterpartyName, counterpartyName.trim());
        }
        if (StringUtils.hasText(summary)) {
            q.like(KingdeeBankVoucherDetail::getSummary, summary.trim());
        }
        if (StringUtils.hasText(remark)) {
            q.like(KingdeeBankVoucherDetail::getRemark, remark.trim());
        }
        q.orderByDesc(KingdeeBankVoucherDetail::getBookkeepingDate)
                .orderByDesc(KingdeeBankVoucherDetail::getTradeTime)
                .orderByDesc(KingdeeBankVoucherDetail::getId);
        return mapper.selectList(q);
    }

    public List<KingdeeBankVoucherDetail> search(String channelKey,
                                                 String bookkeepingDateStart,
                                                 String bookkeepingDateEnd,
                                                 String voucherType,
                                                 Integer writeStatus,
                                                 String counterpartyName,
                                                 String summary,
                                                 String remark) {
        return search(channelKey, bookkeepingDateStart, bookkeepingDateEnd, voucherType, writeStatus, null,
                counterpartyName, summary, remark);
    }

    public List<KingdeeBankVoucherDetail> search(String bookkeepingDateStart,
                                                 String bookkeepingDateEnd,
                                                 String voucherType,
                                                 Integer writeStatus,
                                                 String counterpartyName,
                                                 String summary,
                                                 String remark) {
        return search(null, bookkeepingDateStart, bookkeepingDateEnd, voucherType, writeStatus, null,
                counterpartyName, summary, remark);
    }

    public Optional<KingdeeBankVoucherDetail> findById(Long id) {
        KingdeeBankVoucherDetail e = mapper.selectById(id);
        if (e == null || (e.getDeleted() != null && e.getDeleted() != 0)) {
            return Optional.empty();
        }
        return Optional.of(e);
    }

    public List<KingdeeBankVoucherDetail> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectBatchIds(ids).stream()
                .filter(e -> e.getDeleted() == null || e.getDeleted() == 0)
                .collect(Collectors.toList());
    }

    public KingdeeBankVoucherDetail update(KingdeeBankVoucherDetail entity) {
        KingdeeBankVoucherDetail existing = findById(entity.getId())
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        existing.setCounterpartyName(entity.getCounterpartyName());
        existing.setSummary(entity.getSummary());
        existing.setRemark(entity.getRemark());
        existing.setDebitAmount(entity.getDebitAmount());
        existing.setCreditAmount(entity.getCreditAmount());
        existing.setBookkeepingDate(normalizeDate(entity.getBookkeepingDate()));
        if (StringUtils.hasText(entity.getVoucherType())) {
            existing.setVoucherType(entity.getVoucherType().trim());
            existing.setVoucherTypeName(StringUtils.hasText(entity.getVoucherTypeName())
                    ? entity.getVoucherTypeName().trim()
                    : BankVoucherTypeClassifier.typeName(existing.getVoucherType()));
        } else {
            applyClassification(existing);
        }
        existing.setUpdateTime(LocalDateTime.now());
        mapper.updateById(existing);
        return existing;
    }

    public void deleteById(Long id) {
        findById(id).ifPresent(entity -> {
            entity.setDeleted(1);
            entity.setUpdateTime(LocalDateTime.now());
            mapper.updateById(entity);
        });
    }

    /**
     * 物理删除表内全部明细（含已软删行），不可恢复。
     */
    @Transactional
    public int hardDeleteAll() {
        int count = mapper.delete(new LambdaQueryWrapper<KingdeeBankVoucherDetail>()
                .isNotNull(KingdeeBankVoucherDetail::getId));
        log.warn("一键清空银行明细凭证，物理删除 {} 条", count);
        return count;
    }

    @Transactional
    public Map<String, Object> importExcel(List<KingdeeBankVoucherDetailExcel> rows, String channelKey, String sourceFileName) {
        if (rows == null || rows.isEmpty()) {
            throw new RuntimeException("Excel 无有效数据");
        }
        String key = StringUtils.hasText(channelKey) ? channelKey.trim() : KingdeeBankChannelService.DEFAULT_CHANNEL;
        KingdeeBankChannel channel = channelService.requireByKey(key);

        int removed = dedupeExisting(key);
        if (removed > 0) {
            log.info("导入前清理重复明细 {} 条 channel={}", removed, key);
        }

        int count = 0;
        int created = 0;
        int updated = 0;
        int skippedNoKey = 0;
        int synthesizedSerial = 0;
        String minDate = null;
        String maxDate = null;
        LocalDateTime now = LocalDateTime.now();
        // 本批次已占用流水号，防止同文件弱流水号/空流水号互相覆盖（建行/工行/中行共用）
        java.util.Set<String> usedSerials = new java.util.LinkedHashSet<>();
        int rowIndex = 0;
        for (KingdeeBankVoucherDetailExcel row : rows) {
            rowIndex++;
            if (isBlankRow(row)) {
                continue;
            }
            // 唯一匹配键=交易流水号；无有效流水号时合成稳定号，库中不存在则新增，绝不按同金额覆盖
            String serial = resolveImportSerial(key, row, rowIndex, usedSerials);
            if (serial == null) {
                skippedNoKey++;
                continue;
            }
            if (!serial.equals(trimToNull(row.getTradeSerialNo()))) {
                synthesizedSerial++;
            }
            row.setTradeSerialNo(serial);

            KingdeeBankVoucherDetail entity = findByTradeSerial(key, serial).orElse(null);
            boolean isNew = entity == null;
            if (isNew) {
                entity = new KingdeeBankVoucherDetail();
                entity.setDeleted(0);
                entity.setWriteStatus(0);
                entity.setCreateTime(now);
            }
            entity.setChannelKey(channel.getChannelKey());
            entity.setCompanyCode(channel.getCompanyCode());
            entity.setBankCode(channel.getBankCode());
            entity.setExcelTemplate(channel.getExcelTemplate());
            if (StringUtils.hasText(sourceFileName)) {
                entity.setSourceFileName(sourceFileName.trim());
            }
            fillFromExcel(entity, row);
            // 强制写回解析后的唯一流水号（避免 fill 后仍为空或被弱值覆盖）
            entity.setTradeSerialNo(serial);
            if (!StringUtils.hasText(entity.getAccountNo()) && StringUtils.hasText(channel.getAccountNo())) {
                entity.setAccountNo(channel.getAccountNo());
            }
            if (!StringUtils.hasText(entity.getAccountName()) && StringUtils.hasText(channel.getAccountName())) {
                entity.setAccountName(channel.getAccountName());
            }
            applyClassification(entity);
            entity.setUpdateTime(now);
            if (entity.getId() == null) { mapper.insert(entity); } else { mapper.updateById(entity); }
            count++;
            if (isNew) {
                created++;
            } else {
                updated++;
            }
            String bk = entity.getBookkeepingDate();
            if (StringUtils.hasText(bk)) {
                if (minDate == null || bk.compareTo(minDate) < 0) {
                    minDate = bk;
                }
                if (maxDate == null || bk.compareTo(maxDate) > 0) {
                    maxDate = bk;
                }
            }
        }
        if (skippedNoKey > 0) {
            log.warn("导入跳过无关键字段行 {} 条", skippedNoKey);
        }
        if (synthesizedSerial > 0) {
            log.info("导入为 {} 条明细生成稳定流水号 channel={}", synthesizedSerial, key);
        }
        log.info("银字号导入完成 channel={} total={} created={} updated={} synthesized={}",
                key, count, created, updated, synthesizedSerial);
        if (count <= 0) {
            throw new RuntimeException("未解析到有效流水明细，请确认文件为银行明细导出且渠道模板匹配（当前渠道 "
                    + key + " / " + channel.getExcelTemplate() + "）");
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("count", count);
        result.put("created", created);
        result.put("updated", updated);
        result.put("dateStart", minDate);
        result.put("dateEnd", maxDate);
        result.put("synthesizedSerial", synthesizedSerial);
        return result;
    }

    @Transactional
    public Map<String, Object> importExcel(List<KingdeeBankVoucherDetailExcel> rows) {
        return importExcel(rows, KingdeeBankChannelService.DEFAULT_CHANNEL, null);
    }

    @Transactional
    public int dedupeExisting() {
        return dedupeExisting(null);
    }

    /**
     * 仅按「渠道+交易流水号」清理真重复；禁止按同日期同金额指纹合并，避免不同流水被误删。
     */
    @Transactional
    public int dedupeExisting(String channelKey) {
        List<KingdeeBankVoucherDetail> all = StringUtils.hasText(channelKey)
                ? mapper.selectList(new LambdaQueryWrapper<KingdeeBankVoucherDetail>().eq(KingdeeBankVoucherDetail::getChannelKey, channelKey.trim()).eq(KingdeeBankVoucherDetail::getDeleted, 0).orderByAsc(KingdeeBankVoucherDetail::getId))
                : mapper.selectList(new LambdaQueryWrapper<KingdeeBankVoucherDetail>().eq(KingdeeBankVoucherDetail::getDeleted, 0).orderByAsc(KingdeeBankVoucherDetail::getId));
        java.util.Set<Long> deleteIds = new java.util.LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();

        java.util.Map<String, KingdeeBankVoucherDetail> bySerial = new java.util.LinkedHashMap<>();
        for (KingdeeBankVoucherDetail row : all) {
            if (!StringUtils.hasText(row.getTradeSerialNo())) {
                continue;
            }
            String serialKey = nullToEmpty(row.getChannelKey()) + "|" + row.getTradeSerialNo().trim();
            KingdeeBankVoucherDetail existing = bySerial.get(serialKey);
            if (existing == null) {
                bySerial.put(serialKey, row);
            } else if (isBetterThan(row, existing)) {
                deleteIds.add(existing.getId());
                bySerial.put(serialKey, row);
            } else {
                deleteIds.add(row.getId());
            }
        }

        int removed = 0;
        for (Long id : deleteIds) {
            if (id == null) {
                continue;
            }
            findById(id).ifPresent(entity -> {
                entity.setDeleted(1);
                entity.setUpdateTime(now);
                if (entity.getId() == null) { mapper.insert(entity); } else { mapper.updateById(entity); }
            });
            removed++;
        }
        return removed;
    }

    private Optional<KingdeeBankVoucherDetail> findByTradeSerial(String channelKey, String serial) {
        if (StringUtils.hasText(channelKey)) {
            return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<KingdeeBankVoucherDetail>().eq(KingdeeBankVoucherDetail::getChannelKey, channelKey.trim()).eq(KingdeeBankVoucherDetail::getTradeSerialNo, serial).eq(KingdeeBankVoucherDetail::getDeleted, 0).last("LIMIT 1")));
        }
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<KingdeeBankVoucherDetail>().eq(KingdeeBankVoucherDetail::getTradeSerialNo, serial).eq(KingdeeBankVoucherDetail::getDeleted, 0).last("LIMIT 1")));
    }

    /**
     * 为各渠道导入生成稳定唯一流水号：
     * - 真实银行流水号优先；库中无该号则新增，有则更新同一条（不按同金额覆盖）
     * - 摘要/无数字等弱值视为无效（工行历史曾用摘要顶替流水号导致同摘要多笔互相覆盖）
     * - 缺失或弱值时按业务字段合成；本批次内冲突则追加序号，保证同文件多笔都能落库
     */
    private String resolveImportSerial(String channelKey, KingdeeBankVoucherDetailExcel row, int rowIndex,
                                       java.util.Set<String> usedSerials) {
        String raw = trimToNull(row.getTradeSerialNo());
        boolean weak = isWeakSerial(raw, row.getSummary());
        if (weak) {
            raw = firstNonBlank(
                    trimToNull(row.getReceiptNo()),
                    trimToNull(row.getBankVoucherNo()),
                    trimToNull(row.getEnterpriseSerialNo()));
            if (isWeakSerial(raw, row.getSummary())) {
                raw = null;
            }
        }
        if (raw == null) {
            raw = buildSyntheticSerial(channelKey, row, rowIndex);
            weak = true;
        }
        if (!weak) {
            usedSerials.add(channelKey + "|" + raw);
            return raw;
        }
        String candidate = raw;
        int suffix = 2;
        while (usedSerials.contains(channelKey + "|" + candidate)) {
            candidate = raw + "#" + suffix++;
        }
        usedSerials.add(channelKey + "|" + candidate);
        return candidate;
    }

    private boolean isWeakSerial(String serial, String summary) {
        if (!StringUtils.hasText(serial)) {
            return true;
        }
        String s = serial.trim();
        // 无数字：如「电子转账」「手续费」等摘要误当作流水号
        if (!s.matches(".*\\d.*")) {
            return true;
        }
        String sum = trimToNull(summary);
        return sum != null && s.equals(sum);
    }

    private String buildSyntheticSerial(String channelKey, KingdeeBankVoucherDetailExcel row, int rowIndex) {
        String basis = nullToEmpty(channelKey)
                + "|" + nullToEmpty(trimToNull(row.getTradeTime()))
                + "|" + nullToEmpty(trimToNull(row.getCounterpartyName()))
                + "|" + nullToEmpty(trimToNull(row.getSummary()))
                + "|" + nullToEmpty(trimToNull(row.getRemark()))
                + "|" + nullToEmpty(trimToNull(row.getDebitAmount()))
                + "|" + nullToEmpty(trimToNull(row.getCreditAmount()))
                + "|" + nullToEmpty(trimToNull(row.getReceiptNo()))
                + "|" + nullToEmpty(trimToNull(row.getBankVoucherNo()))
                + "|" + rowIndex;
        return "AUTO-" + Integer.toHexString(basis.hashCode()).toUpperCase() + "-" + rowIndex;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBetterThan(KingdeeBankVoucherDetail a, KingdeeBankVoucherDetail b) {
        int scoreA = score(a);
        int scoreB = score(b);
        if (scoreA != scoreB) {
            return scoreA > scoreB;
        }
        Long idA = a.getId() == null ? Long.MAX_VALUE : a.getId();
        Long idB = b.getId() == null ? Long.MAX_VALUE : b.getId();
        return idA < idB;
    }

    private int score(KingdeeBankVoucherDetail row) {
        int s = 0;
        if (row.getWriteStatus() != null && row.getWriteStatus() == 1) {
            s += 1000;
        }
        if (StringUtils.hasText(row.getTradeSerialNo())) {
            s += 100;
        }
        if (hasPositiveAmount(row.getDebitAmount()) || hasPositiveAmount(row.getCreditAmount())) {
            s += 10;
        }
        return s;
    }

    private boolean hasPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) != 0;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public void exportExcel(HttpServletResponse response,
                            String channelKey,
                            String bookkeepingDateStart,
                            String bookkeepingDateEnd,
                            String voucherType,
                            Integer writeStatus,
                            Boolean writable) throws IOException {
        List<KingdeeBankVoucherDetailExcel> rows = search(
                channelKey, bookkeepingDateStart, bookkeepingDateEnd, voucherType, writeStatus, writable,
                null, null, null)
                .stream()
                .map(this::toExcel)
                .collect(Collectors.toList());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("银行明细凭证.xlsx", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), KingdeeBankVoucherDetailExcel.class)
                .sheet("银行明细")
                .doWrite(rows);
    }

    public void exportExcel(HttpServletResponse response,
                            String channelKey,
                            String bookkeepingDateStart,
                            String bookkeepingDateEnd,
                            String voucherType,
                            Integer writeStatus) throws IOException {
        exportExcel(response, channelKey, bookkeepingDateStart, bookkeepingDateEnd, voucherType, writeStatus, null);
    }

    public void exportExcel(HttpServletResponse response,
                            String bookkeepingDateStart,
                            String bookkeepingDateEnd,
                            String voucherType,
                            Integer writeStatus) throws IOException {
        exportExcel(response, null, bookkeepingDateStart, bookkeepingDateEnd, voucherType, writeStatus, null);
    }

    public void markWritten(List<Long> ids, String voucherId, String voucherNo,
                            String voucherWord, Long accountId) {
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            findById(id).ifPresent(entity -> {
                entity.setWriteStatus(1);
                entity.setKingdeeVoucherId(voucherId);
                entity.setKingdeeVoucherNo(voucherNo);
                entity.setKingdeeVoucherWord(voucherWord);
                entity.setPlatformWriteTime(now);
                entity.setWriteError(null);
                entity.setKingdeeAccountId(accountId);
                // 同步查重态，避免列表因「金蝶无」仍显示未写入
                entity.setKingdeeExistStatus(1);
                entity.setKingdeeExistVoucherNo(voucherNo);
                entity.setKingdeeExistVoucherWord(voucherWord);
                entity.setKingdeeExistCheckedAt(now);
                entity.setUpdateTime(now);
                if (entity.getId() == null) { mapper.insert(entity); } else { mapper.updateById(entity); }
            });
        }
    }

    public void markWriteError(List<Long> ids, String error) {
        String truncated = error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error);
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            findById(id).ifPresent(entity -> {
                entity.setWriteStatus(2);
                entity.setWriteError(truncated);
                entity.setUpdateTime(now);
                if (entity.getId() == null) { mapper.insert(entity); } else { mapper.updateById(entity); }
            });
        }
    }

    private void fillFromExcel(KingdeeBankVoucherDetail entity, KingdeeBankVoucherDetailExcel row) {
        entity.setAccountNo(trimToNull(row.getAccountNo()));
        entity.setAccountName(trimToNull(row.getAccountName()));
        entity.setTradeTime(trimToNull(row.getTradeTime()));
        BigDecimal debit = parseAmount(row.getDebitAmount());
        BigDecimal credit = parseAmount(row.getCreditAmount());
        BigDecimal balance = parseAmount(row.getBalance());
        if (debit != null) {
            entity.setDebitAmount(debit);
        }
        if (credit != null) {
            entity.setCreditAmount(credit);
        }
        if (balance != null) {
            entity.setBalance(balance);
        }
        if (entity.getDebitAmount() == null) {
            entity.setDebitAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        if (entity.getCreditAmount() == null) {
            entity.setCreditAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        entity.setCurrency(trimToNull(row.getCurrency()));
        entity.setCounterpartyName(trimToNull(row.getCounterpartyName()));
        entity.setCounterpartyAccount(trimToNull(row.getCounterpartyAccount()));
        entity.setCounterpartyBank(trimToNull(row.getCounterpartyBank()));
        String bookkeepingDate = normalizeDate(row.getBookkeepingDate());
        if (!StringUtils.hasText(bookkeepingDate)) {
            bookkeepingDate = normalizeDate(row.getTradeTime());
        }
        entity.setBookkeepingDate(bookkeepingDate);
        entity.setSummary(trimToNull(row.getSummary()));
        entity.setRemark(trimToNull(row.getRemark()));
        entity.setPurpose(trimToNull(row.getPurpose()));
        entity.setReceiptNo(trimToNull(row.getReceiptNo()));
        entity.setDirectionFlag(trimToNull(row.getDirectionFlag()));
        BigDecimal rawAmount = parseAmount(row.getRawAmount());
        if (rawAmount != null) {
            entity.setRawAmount(rawAmount);
        }
        if (!StringUtils.hasText(entity.getTradeSerialNo())) {
            entity.setTradeSerialNo(trimToNull(row.getTradeSerialNo()));
        }
        entity.setEnterpriseSerialNo(trimToNull(row.getEnterpriseSerialNo()));
        entity.setVoucherKind(trimToNull(row.getVoucherKind()));
        entity.setBankVoucherNo(trimToNull(row.getBankVoucherNo()));
    }

    private void applyClassification(KingdeeBankVoucherDetail entity) {
        String ownName = resolveOwnCompanyName(entity);
        String type = BankVoucherTypeClassifier.classify(
                entity.getSummary(),
                joinRemark(entity.getRemark(), entity.getPurpose()),
                entity.getDebitAmount(),
                entity.getCreditAmount(),
                entity.getCounterpartyName(),
                ownName);
        entity.setVoucherType(type);
        entity.setVoucherTypeName(BankVoucherTypeClassifier.typeName(type));
    }

    /** 本公司户名：明细账户名优先，否则取渠道配置 */
    private String resolveOwnCompanyName(KingdeeBankVoucherDetail entity) {
        if (entity != null && StringUtils.hasText(entity.getAccountName())) {
            return entity.getAccountName().trim();
        }
        if (entity != null && StringUtils.hasText(entity.getChannelKey())) {
            try {
                KingdeeBankChannel channel = channelService.requireByKey(entity.getChannelKey());
                if (channel != null && StringUtils.hasText(channel.getAccountName())) {
                    return channel.getAccountName().trim();
                }
            } catch (RuntimeException ignored) {
                // ignore
            }
        }
        return "广东汾源酒业有限公司";
    }

    private String joinRemark(String remark, String purpose) {
        if (!StringUtils.hasText(purpose)) {
            return remark;
        }
        if (!StringUtils.hasText(remark)) {
            return purpose;
        }
        return remark + " " + purpose;
    }

    private KingdeeBankVoucherDetailExcel toExcel(KingdeeBankVoucherDetail entity) {
        KingdeeBankVoucherDetailExcel excel = new KingdeeBankVoucherDetailExcel();
        excel.setAccountNo(entity.getAccountNo());
        excel.setAccountName(entity.getAccountName());
        excel.setTradeTime(entity.getTradeTime());
        excel.setDebitAmount(formatAmount(entity.getDebitAmount()));
        excel.setCreditAmount(formatAmount(entity.getCreditAmount()));
        excel.setBalance(formatAmount(entity.getBalance()));
        excel.setCurrency(entity.getCurrency());
        excel.setCounterpartyName(entity.getCounterpartyName());
        excel.setCounterpartyAccount(entity.getCounterpartyAccount());
        excel.setCounterpartyBank(entity.getCounterpartyBank());
        excel.setBookkeepingDate(entity.getBookkeepingDate());
        excel.setSummary(entity.getSummary());
        excel.setRemark(entity.getRemark());
        excel.setTradeSerialNo(entity.getTradeSerialNo());
        excel.setEnterpriseSerialNo(entity.getEnterpriseSerialNo());
        excel.setVoucherKind(entity.getVoucherKind());
        excel.setBankVoucherNo(entity.getBankVoucherNo());
        excel.setVoucherTypeName(entity.getVoucherTypeName());
        excel.setWriteStatusLabel(writeStatusLabel(entity.getWriteStatus()));
        excel.setKingdeeVoucherNo(entity.getKingdeeVoucherNo());
        return excel;
    }

    private String writeStatusLabel(Integer status) {
        if (status == null || status == 0) {
            return "未写入";
        }
        if (status == 1) {
            return "已写入";
        }
        if (status == 2) {
            return "失败";
        }
        return String.valueOf(status);
    }

    private boolean isBlankRow(KingdeeBankVoucherDetailExcel row) {
        return !StringUtils.hasText(row.getTradeSerialNo())
                && !StringUtils.hasText(row.getTradeTime())
                && !StringUtils.hasText(row.getBookkeepingDate())
                && !StringUtils.hasText(row.getDebitAmount())
                && !StringUtils.hasText(row.getCreditAmount())
                && !StringUtils.hasText(row.getRawAmount());
    }

    private BigDecimal parseAmount(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim()
                .replace(",", "")
                .replace("，", "")
                .replace(" ", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace("元", "");
        if (value.isEmpty() || "-".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            // 工行大写+￥格式兜底
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(-?[0-9]+(?:\\.[0-9]+)?)")
                    .matcher(value);
            if (m.find()) {
                try {
                    return new BigDecimal(m.group(1)).setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
            log.warn("无法解析金额: {}", raw);
            return null;
        }
    }

    private String formatAmount(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizeDate(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 8);
        }
        return value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
