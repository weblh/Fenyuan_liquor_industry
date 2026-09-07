package com.fenyuan.liquor.modules.finance.kingdeevoucher.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.finance.kingdeevoucher.entity.FinKingdeeVoucher;
import com.fenyuan.liquor.modules.finance.kingdeevoucher.mapper.FinKingdeeVoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinKingdeeVoucherService {

    private final FinKingdeeVoucherMapper mapper;

    public PageResult<FinKingdeeVoucher> page(long current, long size, String bankFlowNo, String counterparty) {
        Page<FinKingdeeVoucher> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<FinKingdeeVoucher>()
                        .like(StrUtil.isNotBlank(bankFlowNo), FinKingdeeVoucher::getBankFlowNo, bankFlowNo)
                        .like(StrUtil.isNotBlank(counterparty), FinKingdeeVoucher::getCounterparty, counterparty)
                        .orderByDesc(FinKingdeeVoucher::getId));
        return PageResult.of(page);
    }

    public FinKingdeeVoucher getById(Long id) {
        FinKingdeeVoucher entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(FinKingdeeVoucher entity) {
        if (entity.getSyncStatus() == null) {
            entity.setSyncStatus(0);
        }
        mapper.insert(entity);
    }

    public void update(Long id, FinKingdeeVoucher entity) {
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
