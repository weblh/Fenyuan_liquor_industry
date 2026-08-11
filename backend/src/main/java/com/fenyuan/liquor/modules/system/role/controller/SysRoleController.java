package com.fenyuan.liquor.modules.system.role.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.system.role.dto.RoleDTO;
import com.fenyuan.liquor.modules.system.role.entity.SysRole;
import com.fenyuan.liquor.modules.system.role.service.SysRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "角色管理")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @ApiOperation("分页查询角色")
    @RequiresPermission("system:role:list")
    @GetMapping
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(sysRoleService.page(current, size, name, status));
    }

    @ApiOperation("全部角色")
    @GetMapping("/all")
    public Result<List<SysRole>> all() {
        return Result.ok(sysRoleService.listAll());
    }

    @ApiOperation("角色详情")
    @RequiresPermission("system:role:list")
    @GetMapping("/{id}")
    public Result<SysRole> get(@PathVariable Long id) {
        return Result.ok(sysRoleService.getById(id));
    }

    @ApiOperation("新增角色")
    @RequiresPermission("system:role:add")
    @Log(value = "新增角色", module = "角色管理")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody RoleDTO dto) {
        sysRoleService.create(dto);
        return Result.ok();
    }

    @ApiOperation("更新角色")
    @RequiresPermission("system:role:edit")
    @Log(value = "更新角色", module = "角色管理")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        sysRoleService.update(id, dto);
        return Result.ok();
    }

    @ApiOperation("删除角色")
    @RequiresPermission("system:role:delete")
    @Log(value = "删除角色", module = "角色管理")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        sysRoleService.deleteByIds(ids);
        return Result.ok();
    }

    @ApiOperation("更新角色状态")
    @RequiresPermission("system:role:edit")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        sysRoleService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @ApiOperation("获取角色权限")
    @RequiresPermission("system:role:permission")
    @GetMapping("/{id}/permissions")
    public Result<Map<String, Object>> getPermissions(@PathVariable Long id) {
        return Result.ok(sysRoleService.getPermissions(id));
    }

    @ApiOperation("分配角色权限")
    @RequiresPermission("system:role:permission")
    @Log(value = "分配角色权限", module = "角色管理")
    @PutMapping("/{id}/permissions")
    public Result<Void> updatePermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        sysRoleService.updatePermissions(id, body.get("menuIds"));
        return Result.ok();
    }
}
