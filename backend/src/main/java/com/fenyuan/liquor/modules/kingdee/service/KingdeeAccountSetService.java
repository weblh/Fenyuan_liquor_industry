package com.fenyuan.liquor.modules.kingdee.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeAccountSet;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeCredential;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeAccountSetMapper;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeCredentialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KingdeeAccountSetService {

    private final KingdeeAccountSetMapper mapper;
    private final KingdeeCredentialMapper credentialMapper;

    public PageResult<KingdeeAccountSet> page(long current, long size, String accountName) {
        Page<KingdeeAccountSet> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<KingdeeAccountSet>()
                        .like(StrUtil.isNotBlank(accountName), KingdeeAccountSet::getAccountName, accountName)
                        .orderByDesc(KingdeeAccountSet::getIsDefault)
                        .orderByDesc(KingdeeAccountSet::getId));
        fillCredentialNames(page.getRecords());
        return PageResult.of(page);
    }

    public List<KingdeeAccountSet> listEnabled() {
        List<KingdeeAccountSet> list = mapper.selectList(new LambdaQueryWrapper<KingdeeAccountSet>()
                .eq(KingdeeAccountSet::getStatus, 1)
                .orderByDesc(KingdeeAccountSet::getIsDefault)
                .orderByDesc(KingdeeAccountSet::getId));
        fillCredentialNames(list);
        return list;
    }

    public KingdeeAccountSet getById(Long id) {
        KingdeeAccountSet entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("账套不存在");
        }
        fillCredentialNames(java.util.Collections.singletonList(entity));
        return entity;
    }

    public KingdeeAccountSet getDefault() {
        KingdeeAccountSet entity = mapper.selectOne(new LambdaQueryWrapper<KingdeeAccountSet>()
                .eq(KingdeeAccountSet::getIsDefault, 1)
                .eq(KingdeeAccountSet::getStatus, 1)
                .last("LIMIT 1"));
        if (entity != null) {
            fillCredentialNames(java.util.Collections.singletonList(entity));
        }
        return entity;
    }

    public void create(KingdeeAccountSet entity) {
        validateCredential(entity.getCredentialId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getIsDefault() == null) {
            entity.setIsDefault(0);
        }
        if (entity.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        mapper.insert(entity);
    }

    public void update(Long id, KingdeeAccountSet entity) {
        KingdeeAccountSet existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("账套不存在");
        }
        validateCredential(entity.getCredentialId());
        entity.setId(id);
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlagExcept(id);
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
    public void setDefault(Long id) {
        KingdeeAccountSet entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("账套不存在");
        }
        clearDefaultFlag();
        entity.setIsDefault(1);
        entity.setStatus(1);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
    }

    @Transactional
    public void toggleStatus(Long id) {
        KingdeeAccountSet entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("账套不存在");
        }
        entity.setStatus(entity.getStatus() != null && entity.getStatus() == 1 ? 0 : 1);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
    }

    private void validateCredential(Long credentialId) {
        if (credentialId == null) {
            throw new BusinessException("请选择关联账号");
        }
        KingdeeCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            throw new BusinessException("关联账号不存在");
        }
    }

    private void fillCredentialNames(List<KingdeeAccountSet> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> credentialIds = records.stream()
                .map(KingdeeAccountSet::getCredentialId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (credentialIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = credentialMapper.selectBatchIds(credentialIds).stream()
                .collect(Collectors.toMap(KingdeeCredential::getId, KingdeeCredential::getName, (a, b) -> a));
        records.forEach(r -> r.setCredentialName(nameMap.get(r.getCredentialId())));
    }

    private void clearDefaultFlag() {
        LambdaUpdateWrapper<KingdeeAccountSet> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(KingdeeAccountSet::getIsDefault, 0);
        mapper.update(null, wrapper);
    }

    private void clearDefaultFlagExcept(Long id) {
        LambdaUpdateWrapper<KingdeeAccountSet> wrapper = new LambdaUpdateWrapper<>();
        wrapper.ne(KingdeeAccountSet::getId, id);
        wrapper.set(KingdeeAccountSet::getIsDefault, 0);
        mapper.update(null, wrapper);
    }
}
