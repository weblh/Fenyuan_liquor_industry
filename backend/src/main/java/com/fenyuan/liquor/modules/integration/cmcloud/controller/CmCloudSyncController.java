package com.fenyuan.liquor.modules.integration.cmcloud.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.config.CmCloudProperties;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.integration.cmcloud.client.CmCloudApiClient;
import com.fenyuan.liquor.modules.integration.cmcloud.dto.CmCloudBillSyncResult;
import com.fenyuan.liquor.modules.integration.cmcloud.dto.CmCloudSyncResult;
import com.fenyuan.liquor.modules.integration.cmcloud.service.CmCloudBillSyncService;
import com.fenyuan.liquor.modules.integration.cmcloud.service.CmCloudSyncService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(tags = "管家婆CMCloud同步")
@RestController
@RequestMapping("/api/integration/cmcloud")
@RequiredArgsConstructor
public class CmCloudSyncController {

    private final CmCloudSyncService syncService;
    private final CmCloudBillSyncService billSyncService;
    private final CmCloudApiClient apiClient;
    private final CmCloudProperties properties;

    @ApiOperation("检测财贸 API 是否可达")
    @GetMapping("/api-status")
    public Result<Map<String, Object>> apiStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("apiBaseUrl", properties.getApiBaseUrl());
        data.put("apiMode", properties.getApiMode());
        boolean reachable = false;
        String message;
        try {
            apiClient.ping();
            reachable = true;
            message = "财贸 API 可用，可一键拉取销售/库存到本地库";
        } catch (Exception e) {
            message = e.getMessage();
            log.info("财贸 API 不可用: {}", message);
        }
        data.put("reachable", reachable);
        data.put("message", message);
        return Result.ok(data);
    }

    @ApiOperation("从管家婆本地缓存同步客户/商品/比价/新开客户")
    @RequiresPermission("business:cmcloud:sync")
    @Log(value = "同步管家婆基础资料", module = "管家婆同步")
    @PostMapping("/sync-master")
    public Result<CmCloudSyncResult> syncMaster() {
        return Result.ok(syncService.syncMasterData());
    }

    @ApiOperation("从财贸 API 同步销售单/库存并写入本地业务表")
    @RequiresPermission("business:cmcloud:sync")
    @Log(value = "同步管家婆销售库存单据", module = "管家婆同步")
    @PostMapping("/sync-bills")
    public Result<CmCloudBillSyncResult> syncBills() {
        return Result.ok(billSyncService.syncBills());
    }

    @ApiOperation("导入管家婆「销售明细表」Excel")
    @RequiresPermission("business:cmcloud:sync")
    @Log(value = "导入管家婆销售明细Excel", module = "管家婆同步")
    @PostMapping("/import-sales-excel")
    public Result<CmCloudBillSyncResult> importSalesExcel(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.ok(billSyncService.importSalesExcel(file.getInputStream()));
    }

    @ApiOperation("导入管家婆「存货库存详情」Excel（支持按存货汇总 / 分仓库）")
    @RequiresPermission("business:cmcloud:sync")
    @Log(value = "导入管家婆库存详情Excel", module = "管家婆同步")
    @PostMapping("/import-inventory-excel")
    public Result<Map<String, Object>> importInventoryExcel(@RequestParam("file") MultipartFile file) throws Exception {
        int n = billSyncService.importInventoryExcel(file.getInputStream());
        Map<String, Object> data = new HashMap<>();
        data.put("inventoryUpdated", n);
        return Result.ok(data);
    }
}
