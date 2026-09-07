package com.fenyuan.liquor.modules.business.customermaintain.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.modules.business.common.BizErpRules;
import com.fenyuan.liquor.modules.business.customermaintain.entity.BizCustomerMaintain;
import com.fenyuan.liquor.modules.business.customermaintain.mapper.BizCustomerMaintainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizCustomerMaintainService {

    private static final int ALERT_DAYS = 60;

    private final BizCustomerMaintainMapper mapper;

    public PageResult<BizCustomerMaintain> page(long current, long size, String customerName) {
        Page<BizCustomerMaintain> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BizCustomerMaintain>()
                        .like(StrUtil.isNotBlank(customerName), BizCustomerMaintain::getCustomerName, customerName)
                        .likeRight(BizCustomerMaintain::getRemark, BizErpRules.ERP_SYNC_MARK)
                        .orderByDesc(BizCustomerMaintain::getDaysSincePurchase)
                        .orderByDesc(BizCustomerMaintain::getId));
        return PageResult.of(page);
    }

    public List<BizCustomerMaintain> listAlerts() {
        return mapper.selectList(new LambdaQueryWrapper<BizCustomerMaintain>()
                .eq(BizCustomerMaintain::getAlertStatus, 1)
                .likeRight(BizCustomerMaintain::getRemark, BizErpRules.ERP_SYNC_MARK)
                .isNotNull(BizCustomerMaintain::getDaysSincePurchase)
                .ge(BizCustomerMaintain::getDaysSincePurchase, ALERT_DAYS)
                .orderByDesc(BizCustomerMaintain::getDaysSincePurchase)
                .last("LIMIT 20"));
    }

    public BizCustomerMaintain getById(Long id) {
        BizCustomerMaintain entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("记录不存在");
        }
        return entity;
    }

    public void create(BizCustomerMaintain entity) {
        normalizeAlert(entity);
        mapper.insert(entity);
    }

    public void update(Long id, BizCustomerMaintain entity) {
        getById(id);
        entity.setId(id);
        normalizeAlert(entity);
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

    private void normalizeAlert(BizCustomerMaintain entity) {
        if (StrUtil.isNotBlank(entity.getLastPurchaseDate()) && entity.getDaysSincePurchase() == null) {
            try {
                LocalDate last = LocalDate.parse(entity.getLastPurchaseDate().trim().substring(0, 10));
                long days = ChronoUnit.DAYS.between(last, LocalDate.now());
                entity.setDaysSincePurchase((int) Math.max(days, 0));
            } catch (DateTimeParseException | StringIndexOutOfBoundsException ignored) {
                // keep manual days
            }
        }
        Integer days = entity.getDaysSincePurchase();
        if (days != null) {
            entity.setAlertStatus(days >= ALERT_DAYS ? 1 : 0);
        } else if (entity.getAlertStatus() == null) {
            entity.setAlertStatus(0);
        }
    }
}
