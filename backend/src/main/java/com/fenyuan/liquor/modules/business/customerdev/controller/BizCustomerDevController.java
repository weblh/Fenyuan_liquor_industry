package com.fenyuan.liquor.modules.business.customerdev.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.customerdev.entity.BizCustomerDev;
import com.fenyuan.liquor.modules.business.customerdev.service.BizCustomerDevService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "客户开发")
@RestController
@RequestMapping("/api/business/customer-devs")
@RequiredArgsConstructor
public class BizCustomerDevController {

    private final BizCustomerDevService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:customerDev:list")
    @GetMapping
    public Result<PageResult<BizCustomerDev>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String name) {
        return Result.ok(service.page(current, size, name));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:customerDev:list")
    @GetMapping("/{id}")
    public Result<BizCustomerDev> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:customerDev:add")
    @Log(value = "新增客户开发", module = "客户开发")
    @PostMapping
    public Result<Void> create(@RequestBody BizCustomerDev entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:customerDev:edit")
    @Log(value = "更新客户开发", module = "客户开发")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizCustomerDev entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:customerDev:delete")
    @Log(value = "删除客户开发", module = "客户开发")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}