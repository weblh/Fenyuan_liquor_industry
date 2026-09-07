package com.fenyuan.liquor.modules.kingdee.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeCredential;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeCredentialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KingdeeCredentialService {

    private final KingdeeCredentialMapper mapper;

    public PageResult<KingdeeCredential> page(long current, long size, String name) {
        Page<KingdeeCredential> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<KingdeeCredential>()
                        .like(StrUtil.isNotBlank(name), KingdeeCredential::getName, name)
                        .orderByDesc(KingdeeCredential::getId));
        page.getRecords().forEach(this::maskPassword);
        return PageResult.of(page);
    }

    public List<KingdeeCredential> listEnabled() {
        List<KingdeeCredential> list = mapper.selectList(new LambdaQueryWrapper<KingdeeCredential>()
                .eq(KingdeeCredential::getStatus, 1)
                .orderByDesc(KingdeeCredential::getId));
        list.forEach(this::maskPassword);
        return list;
    }

    public KingdeeCredential getById(Long id) {
        KingdeeCredential entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("账号不存在");
        }
        maskPassword(entity);
        return entity;
    }

    public KingdeeCredential getByIdWithPassword(Long id) {
        KingdeeCredential entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("账号不存在");
        }
        return entity;
    }

    public void create(KingdeeCredential entity) {
        if (!StringUtils.hasText(entity.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        mapper.insert(entity);
    }

    public void update(Long id, KingdeeCredential entity) {
        KingdeeCredential existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("账号不存在");
        }
        entity.setId(id);
        entity.setUpdateTime(LocalDateTime.now());
        if (!StringUtils.hasText(entity.getPassword()) || "******".equals(entity.getPassword())) {
            entity.setPassword(existing.getPassword());
        }
        mapper.updateById(entity);
    }

    public void deleteByIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return;
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        if (!idList.isEmpty()) {
            mapper.deleteBatchIds(idList);
        }
    }

    @Transactional
    public void toggleStatus(Long id) {
        KingdeeCredential entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("账号不存在");
        }
        entity.setStatus(entity.getStatus() != null && entity.getStatus() == 1 ? 0 : 1);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
    }

    private void maskPassword(KingdeeCredential entity) {
        if (StringUtils.hasText(entity.getPassword())) {
            entity.setPassword("******");
        }
    }
}
