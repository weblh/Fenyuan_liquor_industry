package com.fenyuan.liquor.modules.monitor.log.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.monitor.log.entity.SysLoginLog;
import com.fenyuan.liquor.modules.monitor.log.entity.SysOperLog;
import com.fenyuan.liquor.modules.monitor.log.mapper.SysLoginLogMapper;
import com.fenyuan.liquor.modules.monitor.log.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysOperLogMapper sysOperLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;

    public PageResult<SysOperLog> pageOper(long current, long size, String username, String module) {
        Page<SysOperLog> page = sysOperLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysOperLog>()
                        .like(StrUtil.isNotBlank(username), SysOperLog::getUsername, username)
                        .like(StrUtil.isNotBlank(module), SysOperLog::getModule, module)
                        .orderByDesc(SysOperLog::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<SysLoginLog> pageLogin(long current, long size, String username, Integer status) {
        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysLoginLog>()
                        .like(StrUtil.isNotBlank(username), SysLoginLog::getUsername, username)
                        .eq(status != null, SysLoginLog::getStatus, status)
                        .orderByDesc(SysLoginLog::getLoginTime));
        return PageResult.of(page);
    }
}
