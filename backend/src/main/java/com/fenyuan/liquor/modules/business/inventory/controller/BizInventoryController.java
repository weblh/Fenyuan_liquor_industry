package com.fenyuan.liquor.modules.business.inventory.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.inventory.entity.BizInventory;
import com.fenyuan.liquor.modules.business.inventory.service.BizInventoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "汾源酒库存")
@RestController
@RequestMapping("/api/business/inventories")
@RequiredArgsConstructor
public class BizInventoryController {

    private final BizInventoryService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:inventory:list")
    @GetMapping
    public Result<PageResult<BizInventory>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String productName,
                                              @RequestParam(required = false) String warehouse) {
        return Result.ok(service.page(current, size, productName, warehouse));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:inventory:list")
    @GetMapping("/{id}")
    public Result<BizInventory> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:inventory:add")
    @Log(value = "新增汾源酒库存", module = "汾源酒库存")
    @PostMapping
    public Result<Void> create(@RequestBody BizInventory entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:inventory:edit")
    @Log(value = "更新汾源酒库存", module = "汾源酒库存")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizInventory entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:inventory:delete")
    @Log(value = "删除汾源酒库存", module = "汾源酒库存")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}