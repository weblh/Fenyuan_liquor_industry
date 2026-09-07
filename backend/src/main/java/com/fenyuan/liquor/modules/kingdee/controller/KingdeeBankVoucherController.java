package com.fenyuan.liquor.modules.kingdee.controller;

import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.kingdee.dto.KingdeeBankVoucherInferRequest;
import com.fenyuan.liquor.modules.kingdee.dto.KingdeeBankVoucherWriteRequest;
import com.fenyuan.liquor.modules.kingdee.dto.excel.KingdeeBankVoucherDetailExcel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankVoucherDetail;
import com.fenyuan.liquor.modules.kingdee.service.BankVoucherExcelReader;
import com.fenyuan.liquor.modules.kingdee.service.BankVoucherKingdeeExistService;
import com.fenyuan.liquor.modules.kingdee.service.BankVoucherTypeInferService;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeBankChannelService;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeBankVoucherDetailService;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeBankVoucherWriteService;
import com.fenyuan.liquor.modules.kingdee.service.classifier.BankVoucherTypeClassifier;
import com.fenyuan.liquor.modules.kingdee.vo.BankVoucherKingdeeExistResultVO;
import com.fenyuan.liquor.modules.kingdee.vo.BankVoucherTypeInferResultVO;
import com.fenyuan.liquor.modules.kingdee.vo.KingdeeVoucherSaveResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kingdee/bank-voucher")
@RequiredArgsConstructor
public class KingdeeBankVoucherController {

    private final KingdeeBankVoucherDetailService detailService;
    private final BankVoucherTypeInferService inferService;
    private final BankVoucherKingdeeExistService existService;
    private final KingdeeBankVoucherWriteService writeService;
    private final KingdeeBankChannelService channelService;

    @RequiresPermission("kingdee:bankVoucher:list")
    @GetMapping("/channels")
    public Result<List<KingdeeBankChannel>> channels() {
        return Result.ok("获取成功", channelService.listEnabled());
    }

    @GetMapping("/types")
    public Result<Map<String, String>> types() {
        return Result.ok("获取成功", BankVoucherTypeClassifier.allTypes());
    }

    @GetMapping("/types/meta")
    public Result<List<Map<String, Object>>> typesMeta() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> e : BankVoucherTypeClassifier.allTypes().entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", e.getKey());
            row.put("name", e.getValue());
            row.put("writable", BankVoucherTypeClassifier.isWritable(e.getKey()));
            row.put("reason", BankVoucherTypeClassifier.nonWritableReason(e.getKey()));
            list.add(row);
        }
        return Result.ok("获取成功", list);
    }

    @RequiresPermission("kingdee:bankVoucher:list")
    @GetMapping("/search")
    public Result<List<KingdeeBankVoucherDetail>> search(
            @RequestParam(required = false) String channelKey,
            @RequestParam(required = false) String bookkeepingDateStart,
            @RequestParam(required = false) String bookkeepingDateEnd,
            @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) Integer writeStatus,
            @RequestParam(required = false) Boolean writable,
            @RequestParam(required = false) String counterpartyName,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String remark) {
        try {
            return Result.ok("查询成功",
                    detailService.search(channelKey, bookkeepingDateStart, bookkeepingDateEnd, voucherType,
                            writeStatus, writable, counterpartyName, summary, remark));
        } catch (Exception e) {
            log.error("查询银行明细凭证失败", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PutMapping
    public Result<KingdeeBankVoucherDetail> update(@RequestBody KingdeeBankVoucherDetail entity) {
        try {
            return Result.ok("更新成功", detailService.update(entity));
        } catch (Exception e) {
            log.error("更新银行明细凭证失败", e);
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    /** 一键清空：物理删除银行明细表全部数据（须在 /{id} 之前声明） */
    @DeleteMapping("/clear-all")
    public Result<Map<String, Object>> clearAll() {
        try {
            int count = detailService.hardDeleteAll();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", count);
            return Result.ok("已清空，物理删除 " + count + " 条", data);
        } catch (Exception e) {
            log.error("一键清空银行明细凭证失败", e);
            return Result.fail("清空失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            detailService.deleteById(id);
            return Result.ok("删除成功", null);
        } catch (Exception e) {
            log.error("删除银行明细凭证失败", e);
            return Result.fail("删除失败：" + e.getMessage());
        }
    }

    @RequiresPermission("kingdee:bankVoucher:import")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String channelKey) {
        try {
            String key = StringUtils.hasText(channelKey)
                    ? channelKey.trim()
                    : KingdeeBankChannelService.DEFAULT_CHANNEL;
            KingdeeBankChannel channel = channelService.requireByKey(key);
            List<KingdeeBankVoucherDetailExcel> rows =
                    BankVoucherExcelReader.read(file.getInputStream(), channel);
            Map<String, Object> result = detailService.importExcel(rows, key, file.getOriginalFilename());
            int count = result.get("count") instanceof Number ? ((Number) result.get("count")).intValue() : 0;
            int created = result.get("created") instanceof Number ? ((Number) result.get("created")).intValue() : 0;
            int updated = result.get("updated") instanceof Number ? ((Number) result.get("updated")).intValue() : 0;
            String msg = "导入成功，共 " + count + " 条（新增 " + created + "，更新 " + updated + "）";
            return Result.ok(msg, result);
        } catch (Exception e) {
            log.error("导入银行明细凭证失败", e);
            return Result.fail("导入失败：" + e.getMessage());
        }
    }

    @RequiresPermission("kingdee:bankVoucher:write")
    @PostMapping("/write")
    public Result<KingdeeVoucherSaveResultVO> write(@RequestBody KingdeeBankVoucherWriteRequest request) {
        try {
            KingdeeVoucherSaveResultVO result = writeService.writeSelected(
                    request.getIds(), request.getAccountId(), request.getChannelKey());
            return Result.ok(result.getMessage() != null ? result.getMessage() : "写入成功", result);
        } catch (Exception e) {
            log.error("写入金蝶失败", e);
            return Result.fail("写入失败：" + e.getMessage());
        }
    }

    @PostMapping("/infer-types")
    public Result<BankVoucherTypeInferResultVO> inferTypes(
            @RequestBody(required = false) KingdeeBankVoucherInferRequest request) {
        try {
            KingdeeBankVoucherInferRequest body =
                    request != null ? request : new KingdeeBankVoucherInferRequest();
            boolean dryRun = Boolean.TRUE.equals(body.getDryRun());
            BankVoucherTypeInferResultVO result = inferService.infer(
                    body.getIds(), body.getAccountId(), body.getChannelKey(),
                    body.getAccountingPeriods(), dryRun);
            String msg = "反推完成：匹配 " + result.getMatched() + "/" + result.getTotal()
                    + "，可写 " + result.getWritableMatched()
                    + "，档案缺失 " + result.getMasterDataBlocked()
                    + "，不可写 " + result.getNonWritableMatched()
                    + (dryRun ? "（预览未落库）" : "，已更新 " + result.getUpdated());
            return Result.ok(msg, result);
        } catch (Exception e) {
            log.error("从金蝶反推凭证类型失败", e);
            return Result.fail("反推失败：" + e.getMessage());
        }
    }

    @PostMapping("/check-kingdee-exists")
    public Result<BankVoucherKingdeeExistResultVO> checkKingdeeExists(
            @RequestBody(required = false) KingdeeBankVoucherInferRequest request) {
        try {
            KingdeeBankVoucherInferRequest body =
                    request != null ? request : new KingdeeBankVoucherInferRequest();
            BankVoucherKingdeeExistResultVO result = existService.checkExists(
                    body.getIds(), body.getAccountId(), body.getChannelKey(), body.getAccountingPeriods());
            String msg = "检查完成：金蝶已存在 " + result.getExistsCount()
                    + " 条，不存在 " + result.getNotExistsCount() + " 条";
            return Result.ok(msg, result);
        } catch (Exception e) {
            log.error("检查金蝶重复失败", e);
            return Result.fail("检查失败：" + e.getMessage());
        }
    }
}
