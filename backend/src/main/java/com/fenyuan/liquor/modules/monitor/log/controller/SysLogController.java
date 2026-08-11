package com.fenyuan.liquor.modules.monitor.log.controller;

import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.monitor.log.entity.SysLoginLog;
import com.fenyuan.liquor.modules.monitor.log.entity.SysOperLog;
import com.fenyuan.liquor.modules.monitor.log.service.SysLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "日志管理")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @ApiOperation("操作日志")
    @RequiresPermission("log:oper:list")
    @GetMapping("/oper")
    public Result<PageResult<SysOperLog>> oper(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String username,
                                               @RequestParam(required = false) String module) {
        return Result.ok(sysLogService.pageOper(current, size, username, module));
    }

    @ApiOperation("登录日志")
    @RequiresPermission("log:login:list")
    @GetMapping("/login")
    public Result<PageResult<SysLoginLog>> login(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) Integer status) {
        return Result.ok(sysLogService.pageLogin(current, size, username, status));
    }
}
