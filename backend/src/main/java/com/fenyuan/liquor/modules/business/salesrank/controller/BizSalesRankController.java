package com.fenyuan.liquor.modules.business.salesrank.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.business.salesrank.entity.BizSalesRank;
import com.fenyuan.liquor.modules.business.salesrank.service.BizSalesRankService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "销售排名")
@RestController
@RequestMapping("/api/business/sales-ranks")
@RequiredArgsConstructor
public class BizSalesRankController {

    private final BizSalesRankService service;

    @ApiOperation("分页查询")
    @RequiresPermission("business:salesRank:list")
    @GetMapping
    public Result<PageResult<BizSalesRank>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String companyName) {
        return Result.ok(service.page(current, size, companyName));
    }

    @ApiOperation("详情")
    @RequiresPermission("business:salesRank:list")
    @GetMapping("/{id}")
    public Result<BizSalesRank> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("business:salesRank:add")
    @Log(value = "新增销售排名", module = "销售排名")
    @PostMapping
    public Result<Void> create(@RequestBody BizSalesRank entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("business:salesRank:edit")
    @Log(value = "更新销售排名", module = "销售排名")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BizSalesRank entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("business:salesRank:delete")
    @Log(value = "删除销售排名", module = "销售排名")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }
}