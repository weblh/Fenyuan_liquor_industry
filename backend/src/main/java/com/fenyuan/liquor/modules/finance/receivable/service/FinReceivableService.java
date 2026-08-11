package com.fenyuan.liquor.modules.finance.receivable.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.finance.receivable.entity.FinReceivable;
import com.fenyuan.liquor.modules.finance.receivable.mapper.FinReceivableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinReceivableService {

    private final FinReceivableMapper mapper;

    public PageResult<FinReceivable> page(long current, long size, String name) {
        Page<FinReceivable> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<FinReceivable>()
                        .like(StrUtil.isNotBlank(name), FinReceivable::getName, name)
                        .orderByDesc(FinReceivable::getId));
        return PageResult.of(page);
    }

    public FinReceivable getById(Long id) {
        FinReceivable entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(FinReceivable entity) {
        mapper.insert(entity);
    }

    public void update(Long id, FinReceivable entity) {
        getById(id);
        entity.setId(id);
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
}