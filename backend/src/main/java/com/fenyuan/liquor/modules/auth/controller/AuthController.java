package com.fenyuan.liquor.modules.auth.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.auth.dto.CaptchaResponse;
import com.fenyuan.liquor.modules.auth.dto.LoginRequest;
import com.fenyuan.liquor.modules.auth.dto.LoginResponse;
import com.fenyuan.liquor.modules.auth.dto.UserInfoVO;
import com.fenyuan.liquor.modules.auth.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @ApiOperation("获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaResponse> captcha() {
        return Result.ok(authService.createCaptcha());
    }

    @ApiOperation("登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, httpRequest));
    }

    @ApiOperation("退出登录")
    @Log(value = "退出登录", module = "认证")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @ApiOperation("刷新Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh() {
        return Result.ok(authService.refresh());
    }

    @ApiOperation("当前用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.info());
    }
}
