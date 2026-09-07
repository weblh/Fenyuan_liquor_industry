package com.fenyuan.liquor.modules.business.customermaintain.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.customermaintain.entity.BizCustomerMaintain;
import com.fenyuan.liquor.modules.business.customermaintain.service.BizCustomerMaintainService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "客户维护监管")
@RestController
@RequestMapping("/api/business/customer-maintains")
@RequiredArgsConstructor
public class BizCustomerMaintainController {

    private final BizCustomerMaintainService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:customerMaintain:list")
    @GetMapping
    public Result<PageResult<BizCustomerMaintain>> page(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String customerName) {
        return Result.ok(service.page(current, size, customerName));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:customerMaintain:list")
    @GetMapping("/{id}")
    public Result<BizCustomerMaintain> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:customerMaintain:add")
    @Log(value = "新增客户维护", module = "客户维护监管")
    @PostMapping
    public Result<Void> create(@RequestBody BizCustomerMaintain entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:customerMaintain:edit")
    @Log(value = "更新客户维护", module = "客户维护监管")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizCustomerMaintain entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:customerMaintain:delete")
    @Log(value = "删除客户维护", module = "客户维护监管")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}
