package com.fenyuan.liquor.modules.business.onlinesale.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.onlinesale.entity.BizOnlineSale;
import com.fenyuan.liquor.modules.business.onlinesale.service.BizOnlineSaleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "在线销售管理")
@RestController
@RequestMapping("/api/business/online-sales")
@RequiredArgsConstructor
public class BizOnlineSaleController {

    private final BizOnlineSaleService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:onlineSale:list")
    @GetMapping
    public Result<PageResult<BizOnlineSale>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String periodName) {
        return Result.ok(service.page(current, size, periodName));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:onlineSale:list")
    @GetMapping("/{id}")
    public Result<BizOnlineSale> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:onlineSale:add")
    @Log(value = "新增在线销售管理", module = "在线销售管理")
    @PostMapping
    public Result<Void> create(@RequestBody BizOnlineSale entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:onlineSale:edit")
    @Log(value = "更新在线销售管理", module = "在线销售管理")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizOnlineSale entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:onlineSale:delete")
    @Log(value = "删除在线销售管理", module = "在线销售管理")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}