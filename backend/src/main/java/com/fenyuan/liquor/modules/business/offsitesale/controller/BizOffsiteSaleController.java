package com.fenyuan.liquor.modules.business.offsitesale.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.offsitesale.entity.BizOffsiteSale;
import com.fenyuan.liquor.modules.business.offsitesale.service.BizOffsiteSaleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "异地销售统计")
@RestController
@RequestMapping("/api/business/offsite-sales")
@RequiredArgsConstructor
public class BizOffsiteSaleController {

    private final BizOffsiteSaleService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:offsiteSale:list")
    @GetMapping
    public Result<PageResult<BizOffsiteSale>> page(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String productName,
                                                   @RequestParam(required = false) String province) {
        return Result.ok(service.page(current, size, productName, province));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:offsiteSale:list")
    @GetMapping("/{id}")
    public Result<BizOffsiteSale> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:offsiteSale:add")
    @Log(value = "新增异地销售", module = "异地销售统计")
    @PostMapping
    public Result<Void> create(@RequestBody BizOffsiteSale entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:offsiteSale:edit")
    @Log(value = "更新异地销售", module = "异地销售统计")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizOffsiteSale entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:offsiteSale:delete")
    @Log(value = "删除异地销售", module = "异地销售统计")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}
