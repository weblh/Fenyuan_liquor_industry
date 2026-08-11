package com.fenyuan.liquor.modules.business.inventory.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.inventory.entity.BizInventory;
import com.fenyuan.liquor.modules.business.inventory.mapper.BizInventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizInventoryService {

    private final BizInventoryMapper mapper;

    public PageResult<BizInventory> page(long current, long size, String productName, String warehouse) {
        Page<BizInventory> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizInventory>()
                        .like(StrUtil.isNotBlank(productName), BizInventory::getProductName, productName)
                        .like(StrUtil.isNotBlank(warehouse), BizInventory::getWarehouse, warehouse)
                        .orderByDesc(BizInventory::getId));
        return PageResult.of(page);
    }

    public BizInventory getById(Long id) {
        BizInventory entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizInventory entity) {
        mapper.insert(entity);
    }

    public void update(Long id, BizInventory entity) {
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