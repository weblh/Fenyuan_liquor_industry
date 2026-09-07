package com.fenyuan.liquor.modules.finance.kingdeevoucher.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.finance.kingdeevoucher.entity.FinKingdeeVoucher;
import com.fenyuan.liquor.modules.finance.kingdeevoucher.service.FinKingdeeVoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "管家婆凭证同步")
@RestController
@RequestMapping("/api/finance/kingdee-vouchers")
@RequiredArgsConstructor
public class FinKingdeeVoucherController {

    private final FinKingdeeVoucherService service;

    @ApiOperation("分页查询")
    @RequiresPermission("finance:kingdeeVoucher:list")
    @GetMapping
    public Result<PageResult<FinKingdeeVoucher>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String bankFlowNo,
                                                      @RequestParam(required = false) String counterparty) {
        return Result.ok(service.page(current, size, bankFlowNo, counterparty));
    }

    @ApiOperation("详情")
    @RequiresPermission("finance:kingdeeVoucher:list")
    @GetMapping("/{id}")
    public Result<FinKingdeeVoucher> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("finance:kingdeeVoucher:add")
    @Log(value = "新增管家婆凭证", module = "管家婆凭证同步")
    @PostMapping
    public Result<Void> create(@RequestBody FinKingdeeVoucher entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("finance:kingdeeVoucher:edit")
    @Log(value = "更新管家婆凭证", module = "管家婆凭证同步")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FinKingdeeVoucher entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("finance:kingdeeVoucher:delete")
    @Log(value = "删除管家婆凭证", module = "管家婆凭证同步")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}
