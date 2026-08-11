package com.fenyuan.liquor.modules.system.user.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.system.user.dto.UserDTO;
import com.fenyuan.liquor.modules.system.user.dto.UserVO;
import com.fenyuan.liquor.modules.system.user.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @ApiOperation("分页查询用户")
    @RequiresPermission("system:user:list")
    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long deptId) {
        return Result.ok(sysUserService.page(current, size, username, status, deptId));
    }

    @ApiOperation("用户详情")
    @RequiresPermission("system:user:list")
    @GetMapping("/{id}")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.ok(sysUserService.getById(id));
    }

    @ApiOperation("新增用户")
    @RequiresPermission("system:user:add")
    @Log(value = "新增用户", module = "用户管理")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody UserDTO dto) {
        sysUserService.create(dto);
        return Result.ok();
    }

    @ApiOperation("更新用户")
    @RequiresPermission("system:user:edit")
    @Log(value = "更新用户", module = "用户管理")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        sysUserService.update(id, dto);
        return Result.ok();
    }

    @ApiOperation("删除用户")
    @RequiresPermission("system:user:delete")
    @Log(value = "删除用户", module = "用户管理")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        sysUserService.deleteByIds(ids);
        return Result.ok();
    }

    @ApiOperation("更新用户状态")
    @RequiresPermission("system:user:edit")
    @Log(value = "更新用户状态", module = "用户管理")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        sysUserService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @ApiOperation("重置密码")
    @RequiresPermission("system:user:resetPwd")
    @Log(value = "重置密码", module = "用户管理")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String password = body == null ? null : body.get("password");
        sysUserService.resetPassword(id, password);
        return Result.ok();
    }
}
