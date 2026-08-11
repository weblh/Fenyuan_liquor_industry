package com.fenyuan.liquor.modules.business.salesrank.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.salesrank.entity.BizSalesRank;
import com.fenyuan.liquor.modules.business.salesrank.mapper.BizSalesRankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizSalesRankService {

    private final BizSalesRankMapper mapper;

    public PageResult<BizSalesRank> page(long current, long size, String companyName) {
        Page<BizSalesRank> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizSalesRank>()
                        .like(StrUtil.isNotBlank(companyName), BizSalesRank::getCompanyName, companyName)
                        .orderByDesc(BizSalesRank::getId));
        return PageResult.of(page);
    }

    public BizSalesRank getById(Long id) {
        BizSalesRank entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizSalesRank entity) {
        mapper.insert(entity);
    }

    public void update(Long id, BizSalesRank entity) {
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