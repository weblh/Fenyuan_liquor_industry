package com.fenyuan.liquor.modules.business.offsitesale.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.common.BizErpRules;
import com.fenyuan.liquor.modules.business.offsitesale.entity.BizOffsiteSale;
import com.fenyuan.liquor.modules.business.offsitesale.mapper.BizOffsiteSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizOffsiteSaleService {

    private final BizOffsiteSaleMapper mapper;

    public PageResult<BizOffsiteSale> page(long current, long size, String productName, String province) {
        Page<BizOffsiteSale> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizOffsiteSale>()
                        .like(StrUtil.isNotBlank(productName), BizOffsiteSale::getProductName, productName)
                        .like(StrUtil.isNotBlank(province), BizOffsiteSale::getProvince, province)
                        .orderByDesc(BizOffsiteSale::getId));
        List<BizOffsiteSale> filtered = page.getRecords().stream()
                .filter(this::matchesOffsiteRule)
                .collect(Collectors.toList());
        page.setRecords(filtered);
        return PageResult.of(page);
    }

    public List<BizOffsiteSale> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<BizOffsiteSale>().orderByDesc(BizOffsiteSale::getQuantity))
                .stream()
                .filter(this::matchesOffsiteRule)
                .collect(Collectors.toList());
    }

    private boolean matchesOffsiteRule(BizOffsiteSale item) {
        if (!BizErpRules.matchesAnyOffsiteProduct(item.getProductName())) {
            return false;
        }
        return BizErpRules.isOutsideGuangzhou(item.getProvince(), item.getCity(), item.getAddress());
    }

    public BizOffsiteSale getById(Long id) {
        BizOffsiteSale entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizOffsiteSale entity) {
        mapper.insert(entity);
    }

    public void update(Long id, BizOffsiteSale entity) {
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
