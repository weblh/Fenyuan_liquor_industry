package com.fenyuan.liquor.modules.settings.config.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.settings.config.entity.SysConfig;
import com.fenyuan.liquor.modules.settings.config.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    public PageResult<SysConfig> page(long current, long size, String name, String groupName) {
        Page<SysConfig> page = sysConfigMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysConfig>()
                        .like(StrUtil.isNotBlank(name), SysConfig::getName, name)
                        .eq(StrUtil.isNotBlank(groupName), SysConfig::getGroupName, groupName)
                        .orderByAsc(SysConfig::getId));
        return PageResult.of(page);
    }

    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getStatus, 1)
                .orderByAsc(SysConfig::getId));
    }

    public void update(Long id, SysConfig config) {
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("配置不存在");
        }
        config.setId(id);
        config.setConfigKey(null);
        sysConfigMapper.updateById(config);
    }

    public void updateBatch(List<SysConfig> configs) {
        if (configs == null) {
            return;
        }
        for (SysConfig config : configs) {
            if (config.getId() != null) {
                update(config.getId(), config);
            }
        }
    }
}
