package com.fenyuan.liquor.modules.business.onlinesale.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.onlinesale.entity.BizOnlineSale;
import com.fenyuan.liquor.modules.business.onlinesale.mapper.BizOnlineSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizOnlineSaleService {

    private final BizOnlineSaleMapper mapper;

    public PageResult<BizOnlineSale> page(long current, long size, String periodName) {
        Page<BizOnlineSale> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizOnlineSale>()
                        .like(StrUtil.isNotBlank(periodName), BizOnlineSale::getPeriodName, periodName)
                        .orderByDesc(BizOnlineSale::getId));
        return PageResult.of(page);
    }

    public BizOnlineSale getById(Long id) {
        BizOnlineSale entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizOnlineSale entity) {
        mapper.insert(entity);
    }

    public void update(Long id, BizOnlineSale entity) {
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