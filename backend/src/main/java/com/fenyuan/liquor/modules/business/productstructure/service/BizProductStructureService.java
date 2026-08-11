package com.fenyuan.liquor.modules.business.productstructure.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.productstructure.entity.BizProductStructure;
import com.fenyuan.liquor.modules.business.productstructure.mapper.BizProductStructureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizProductStructureService {

    private final BizProductStructureMapper mapper;

    public PageResult<BizProductStructure> page(long current, long size, String category) {
        Page<BizProductStructure> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizProductStructure>()
                        .like(StrUtil.isNotBlank(category), BizProductStructure::getCategory, category)
                        .orderByDesc(BizProductStructure::getId));
        return PageResult.of(page);
    }

    public BizProductStructure getById(Long id) {
        BizProductStructure entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizProductStructure entity) {
        mapper.insert(entity);
    }

    public void update(Long id, BizProductStructure entity) {
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