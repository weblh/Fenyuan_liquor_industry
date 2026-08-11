package com.fenyuan.liquor.modules.dashboard.controller;

import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.dashboard.dto.DashboardOverviewVO;
import com.fenyuan.liquor.modules.dashboard.service.DashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "首页大屏看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @ApiOperation("经营总览")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.ok(dashboardService.overview());
    }
}
