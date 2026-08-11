package com.fenyuan.liquor.modules.auth.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.utils.JwtUtils;
import com.fenyuan.liquor.common.utils.SecurityUtils;
import com.fenyuan.liquor.modules.auth.dto.CaptchaResponse;
import com.fenyuan.liquor.modules.auth.dto.LoginRequest;
import com.fenyuan.liquor.modules.auth.dto.LoginResponse;
import com.fenyuan.liquor.modules.auth.dto.UserInfoVO;
import com.fenyuan.liquor.modules.monitor.log.entity.SysLoginLog;
import com.fenyuan.liquor.modules.monitor.log.mapper.SysLoginLogMapper;
import com.fenyuan.liquor.modules.system.user.entity.SysUser;
import com.fenyuan.liquor.modules.system.user.mapper.SysUserMapper;
import com.fenyuan.liquor.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final SysLoginLogMapper sysLoginLogMapper;

    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();

    public CaptchaResponse createCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String key = IdUtil.simpleUUID();
        captchaStore.put(key, captcha.getCode());
        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaKey(key);
        response.setCaptchaImage("data:image/png;base64," + captcha.getImageBase64());
        return response;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String code = captchaStore.remove(request.getCaptchaKey());
        if (code == null || !code.equalsIgnoreCase(request.getCaptcha())) {
            saveLoginLog(request.getUsername(), null, 0, "验证码错误", httpRequest);
            throw new BusinessException(400, "验证码错误或已过期");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String token = jwtUtils.generateToken(loginUser.getUserId(), loginUser.getUsername());

            SysUser update = new SysUser();
            update.setId(loginUser.getUserId());
            update.setLoginIp(httpRequest.getRemoteAddr());
            update.setLoginDate(LocalDateTime.now());
            sysUserMapper.updateById(update);

            saveLoginLog(loginUser.getUsername(), loginUser.getUserId(), 1, "登录成功", httpRequest);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserInfo(toUserInfo(loginUser));
            return response;
        } catch (BadCredentialsException e) {
            saveLoginLog(request.getUsername(), null, 0, "用户名或密码错误", httpRequest);
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    public void logout() {
        // JWT 无状态，前端删除 token 即可
    }

    public LoginResponse refresh() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(401, "未登录");
        }
        String token = jwtUtils.generateToken(loginUser.getUserId(), loginUser.getUsername());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(toUserInfo(loginUser));
        return response;
    }

    public UserInfoVO info() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        UserInfoVO vo = toUserInfo(loginUser);
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setRealName(user.getRealName());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setAvatar(user.getAvatar());
            vo.setDeptId(user.getDeptId());
        }
        return vo;
    }

    private UserInfoVO toUserInfo(LoginUser loginUser) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setIsAdmin(loginUser.getIsAdmin());
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setRealName(user.getRealName());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setAvatar(user.getAvatar());
            vo.setDeptId(user.getDeptId());
        }
        return vo;
    }

    private void saveLoginLog(String username, Long userId, int status, String msg, HttpServletRequest request) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setUserId(userId);
        log.setStatus(status);
        log.setMsg(msg);
        log.setIp(request.getRemoteAddr());
        log.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(log);
    }
}
