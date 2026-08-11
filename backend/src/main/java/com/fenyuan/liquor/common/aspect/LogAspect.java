package com.fenyuan.liquor.common.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.utils.SecurityUtils;
import com.fenyuan.liquor.modules.monitor.log.entity.SysOperLog;
import com.fenyuan.liquor.modules.monitor.log.mapper.SysOperLogMapper;
import com.fenyuan.liquor.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperLogMapper sysOperLogMapper;

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint point, RequiresPermission requiresPermission) throws Throwable {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(401, "未登录");
        }
        if (loginUser.isAdmin() || (loginUser.getPermissions() != null
                && (loginUser.getPermissions().contains("*:*:*")
                || loginUser.getPermissions().contains(requiresPermission.value())))) {
            return point.proceed();
        }
        throw new BusinessException(403, "没有访问权限: " + requiresPermission.value());
    }

    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, Log logAnno) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog operLog = new SysOperLog();
        operLog.setModule(logAnno.module());
        operLog.setOperation(logAnno.value());
        MethodSignature signature = (MethodSignature) point.getSignature();
        operLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                operLog.setParams(StrUtil.maxLength(JSONUtil.toJsonStr(args), 2000));
            }
        } catch (Exception ignored) {
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            operLog.setUsername(loginUser.getUsername());
            operLog.setUserId(loginUser.getUserId());
        }
        HttpServletRequest request = currentRequest();
        if (request != null) {
            operLog.setIp(request.getRemoteAddr());
        }
        try {
            Object result = point.proceed();
            operLog.setStatus(1);
            try {
                operLog.setResult(StrUtil.maxLength(JSONUtil.toJsonStr(result), 2000));
            } catch (Exception ignored) {
            }
            return result;
        } catch (Throwable e) {
            operLog.setStatus(0);
            operLog.setErrorMsg(StrUtil.maxLength(e.getMessage(), 2000));
            throw e;
        } finally {
            operLog.setTime(System.currentTimeMillis() - start);
            operLog.setCreateTime(LocalDateTime.now());
            try {
                sysOperLogMapper.insert(operLog);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
