package com.fenyuan.liquor.modules.finance.receivable.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.finance.receivable.entity.FinReceivable;
import com.fenyuan.liquor.modules.finance.receivable.service.FinReceivableService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "应收账款明细")
@RestController
@RequestMapping("/api/finance/receivables")
@RequiredArgsConstructor
public class FinReceivableController {

    private final FinReceivableService service;

    @ApiOperation("分页查询")
    @RequiresPermission("finance:receivable:list")
    @GetMapping
    public Result<PageResult<FinReceivable>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String name) {
        return Result.ok(service.page(current, size, name));
    }

    @ApiOperation("详情")
    @RequiresPermission("finance:receivable:list")
    @GetMapping("/{id}")
    public Result<FinReceivable> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("finance:receivable:add")
    @Log(value = "新增应收账款明细", module = "应收账款明细")
    @PostMapping
    public Result<Void> create(@RequestBody FinReceivable entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("finance:receivable:edit")
    @Log(value = "更新应收账款明细", module = "应收账款明细")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FinReceivable entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("finance:receivable:delete")
    @Log(value = "删除应收账款明细", module = "应收账款明细")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}