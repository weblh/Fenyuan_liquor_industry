package com.fenyuan.liquor.modules.system.menu.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.system.menu.dto.MenuDTO;
import com.fenyuan.liquor.modules.system.menu.entity.SysMenu;
import com.fenyuan.liquor.modules.system.menu.service.SysMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "菜单管理")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @ApiOperation("菜单树")
    @RequiresPermission("system:menu:list")
    @GetMapping
    public Result<List<SysMenu>> tree() {
        return Result.ok(sysMenuService.tree());
    }

    @ApiOperation("路由配置")
    @GetMapping("/routes")
    public Result<List<SysMenu>> routes() {
        return Result.ok(sysMenuService.routes());
    }

    @ApiOperation("当前用户菜单")
    @GetMapping("/user")
    public Result<List<SysMenu>> userMenus() {
        return Result.ok(sysMenuService.userMenus());
    }

    @ApiOperation("菜单详情")
    @RequiresPermission("system:menu:list")
    @GetMapping("/{id}")
    public Result<SysMenu> get(@PathVariable Long id) {
        return Result.ok(sysMenuService.getById(id));
    }

    @ApiOperation("新增菜单")
    @RequiresPermission("system:menu:add")
    @Log(value = "新增菜单", module = "菜单管理")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody MenuDTO dto) {
        sysMenuService.create(dto);
        return Result.ok();
    }

    @ApiOperation("更新菜单")
    @RequiresPermission("system:menu:edit")
    @Log(value = "更新菜单", module = "菜单管理")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MenuDTO dto) {
        sysMenuService.update(id, dto);
        return Result.ok();
    }

    @ApiOperation("删除菜单")
    @RequiresPermission("system:menu:delete")
    @Log(value = "删除菜单", module = "菜单管理")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        sysMenuService.deleteByIds(ids);
        return Result.ok();
    }
}
