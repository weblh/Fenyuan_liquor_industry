package com.fenyuan.liquor.modules.business.productstructure.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.productstructure.entity.BizProductStructure;
import com.fenyuan.liquor.modules.business.productstructure.service.BizProductStructureService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "销售产品结构")
@RestController
@RequestMapping("/api/business/product-structures")
@RequiredArgsConstructor
public class BizProductStructureController {

    private final BizProductStructureService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:productStructure:list")
    @GetMapping
    public Result<PageResult<BizProductStructure>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String category) {
        return Result.ok(service.page(current, size, category));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:productStructure:list")
    @GetMapping("/{id}")
    public Result<BizProductStructure> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:productStructure:add")
    @Log(value = "新增销售产品结构", module = "销售产品结构")
    @PostMapping
    public Result<Void> create(@RequestBody BizProductStructure entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:productStructure:edit")
    @Log(value = "更新销售产品结构", module = "销售产品结构")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizProductStructure entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:productStructure:delete")
    @Log(value = "删除销售产品结构", module = "销售产品结构")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}