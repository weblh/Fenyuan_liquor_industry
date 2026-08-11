package com.fenyuan.liquor.modules.settings.config.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.settings.config.entity.SysConfig;
import com.fenyuan.liquor.modules.settings.config.service.SysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "系统配置")
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @ApiOperation("配置列表")
    @RequiresPermission("system:config:list")
    @GetMapping
    public Result<PageResult<SysConfig>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) String groupName) {
        return Result.ok(sysConfigService.page(current, size, name, groupName));
    }

    @ApiOperation("全部配置")
    @GetMapping("/all")
    public Result<List<SysConfig>> all() {
        return Result.ok(sysConfigService.listAll());
    }

    @ApiOperation("更新配置")
    @RequiresPermission("system:config:edit")
    @Log(value = "更新配置", module = "系统配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysConfig config) {
        sysConfigService.update(id, config);
        return Result.ok();
    }

    @ApiOperation("批量更新配置")
    @RequiresPermission("system:config:edit")
    @Log(value = "批量更新配置", module = "系统配置")
    @PutMapping
    public Result<Void> updateBatch(@RequestBody List<SysConfig> configs) {
        sysConfigService.updateBatch(configs);
        return Result.ok();
    }
}
