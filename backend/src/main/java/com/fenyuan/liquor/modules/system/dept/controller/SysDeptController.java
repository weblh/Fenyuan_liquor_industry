package com.fenyuan.liquor.modules.system.dept.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.system.dept.dto.DeptDTO;
import com.fenyuan.liquor.modules.system.dept.entity.SysDept;
import com.fenyuan.liquor.modules.system.dept.service.SysDeptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "部门管理")
@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService sysDeptService;

    @ApiOperation("部门树")
    @RequiresPermission("system:dept:list")
    @GetMapping
    public Result<List<SysDept>> tree() {
        return Result.ok(sysDeptService.tree());
    }

    @ApiOperation("部门详情")
    @RequiresPermission("system:dept:list")
    @GetMapping("/{id}")
    public Result<SysDept> get(@PathVariable Long id) {
        return Result.ok(sysDeptService.getById(id));
    }

    @ApiOperation("新增部门")
    @RequiresPermission("system:dept:add")
    @Log(value = "新增部门", module = "部门管理")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody DeptDTO dto) {
        sysDeptService.create(dto);
        return Result.ok();
    }

    @ApiOperation("更新部门")
    @RequiresPermission("system:dept:edit")
    @Log(value = "更新部门", module = "部门管理")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DeptDTO dto) {
        sysDeptService.update(id, dto);
        return Result.ok();
    }

    @ApiOperation("删除部门")
    @RequiresPermission("system:dept:delete")
    @Log(value = "删除部门", module = "部门管理")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        sysDeptService.deleteByIds(ids);
        return Result.ok();
    }
}
