package com.fenyuan.liquor.modules.business.pricecompare.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.pricecompare.entity.BizPriceCompare;
import com.fenyuan.liquor.modules.business.pricecompare.service.BizPriceCompareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "酒类价格对比")
@RestController
@RequestMapping("/api/business/price-compares")
@RequiredArgsConstructor
public class BizPriceCompareController {

    private final BizPriceCompareService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:priceCompare:list")
    @GetMapping
    public Result<PageResult<BizPriceCompare>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String productName) {
        return Result.ok(service.page(current, size, productName));
    }

    @ApiOperation("拉取京东/天猫官方店实时价")
    @RequiresPermission("business:priceCompare:list")
    @Log(value = "拉取线上酒价", module = "酒类价格对比")
    @PostMapping("/crawl-online")
    public Result<Map<String, String>> crawlOnline() {
        return Result.ok(service.crawlOnlinePrices());
    }

    @ApiOperation("详情")
    @RequiresPermission("business:priceCompare:list")
    @GetMapping("/{id}")
    public Result<BizPriceCompare> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:priceCompare:add")
    @Log(value = "新增价格对比", module = "酒类价格对比")
    @PostMapping
    public Result<Void> create(@RequestBody BizPriceCompare entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:priceCompare:edit")
    @Log(value = "更新价格对比", module = "酒类价格对比")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizPriceCompare entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:priceCompare:delete")
    @Log(value = "删除价格对比", module = "酒类价格对比")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}
