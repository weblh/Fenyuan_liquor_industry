package com.fenyuan.liquor.modules.business.customerdev.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.customerdev.entity.BizCustomerDev;
import com.fenyuan.liquor.modules.business.customerdev.mapper.BizCustomerDevMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizCustomerDevService {

    private final BizCustomerDevMapper mapper;

    public PageResult<BizCustomerDev> page(long current, long size, String name) {
        Page<BizCustomerDev> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizCustomerDev>()
                        .like(StrUtil.isNotBlank(name), BizCustomerDev::getName, name)
                        .orderByDesc(BizCustomerDev::getId));
        return PageResult.of(page);
    }

    public BizCustomerDev getById(Long id) {
        BizCustomerDev entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizCustomerDev entity) {
        mapper.insert(entity);
    }

    public void update(Long id, BizCustomerDev entity) {
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