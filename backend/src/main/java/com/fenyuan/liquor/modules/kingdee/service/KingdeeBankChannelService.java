package com.fenyuan.liquor.modules.kingdee.service;

import com.fenyuan.liquor.modules.kingdee.dto.KingdeeAccountContext;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankChannel;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeBankVoucherDetail;
import com.fenyuan.liquor.modules.kingdee.mapper.KingdeeBankChannelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KingdeeBankChannelService {

    public static final String DEFAULT_CHANNEL = "FENYUAN_BOC";

    private final KingdeeBankChannelMapper mapper;

    public List<KingdeeBankChannel> listEnabled() {
        return mapper.selectList(new LambdaQueryWrapper<KingdeeBankChannel>()
                .eq(KingdeeBankChannel::getEnabled, 1)
                .orderByAsc(KingdeeBankChannel::getSortOrder)
                .orderByAsc(KingdeeBankChannel::getId));
    }

    /** 渠道是否扫码免手续费（农商行微信等） */
    public boolean isQrFeeExempt(KingdeeBankChannel channel) {
        if (channel == null) {
            return false;
        }
        if (channel.getQrFeeExempt() != null && channel.getQrFeeExempt() == 1) {
            return true;
        }
        String bank = channel.getBankCode() == null ? "" : channel.getBankCode().trim().toUpperCase();
        String template = channel.getExcelTemplate() == null ? "" : channel.getExcelTemplate().trim().toUpperCase();
        return "RCB".equals(bank) || "RCB_RECEIPT".equals(template);
    }

    public Optional<KingdeeBankChannel> findByKey(String channelKey) {
        if (!StringUtils.hasText(channelKey)) {
            return Optional.empty();
        }
        KingdeeBankChannel row = mapper.selectOne(new LambdaQueryWrapper<KingdeeBankChannel>()
                .eq(KingdeeBankChannel::getChannelKey, channelKey.trim())
                .last("LIMIT 1"));
        return Optional.ofNullable(row);
    }

    public KingdeeBankChannel requireByKey(String channelKey) {
        return findByKey(channelKey)
                .orElseThrow(() -> new RuntimeException("未知银行渠道：" + channelKey));
    }

    public KingdeeBankChannel resolveForDetail(KingdeeBankVoucherDetail detail) {
        if (detail != null && StringUtils.hasText(detail.getChannelKey())) {
            return findByKey(detail.getChannelKey())
                    .orElseThrow(() -> new RuntimeException("未知银行渠道：" + detail.getChannelKey()));
        }
        return findDefaultChannel();
    }

    public KingdeeBankChannel resolveForOperation(String channelKey, List<KingdeeBankVoucherDetail> details) {
        if (StringUtils.hasText(channelKey)) {
            return requireByKey(channelKey.trim());
        }
        if (details != null && !details.isEmpty()) {
            return resolveForDetail(details.get(0));
        }
        return findDefaultChannel();
    }

    /** 优先中行；兼容未迁移的历史键 FENYUAN_CCB；再回落到任一启用渠道 */
    private KingdeeBankChannel findDefaultChannel() {
        Optional<KingdeeBankChannel> boc = findByKey(DEFAULT_CHANNEL);
        if (boc.isPresent()) {
            return boc.get();
        }
        Optional<KingdeeBankChannel> legacy = findByKey("FENYUAN_CCB");
        if (legacy.isPresent()) {
            return legacy.get();
        }
        List<KingdeeBankChannel> enabled = listEnabled();
        if (!enabled.isEmpty()) {
            return enabled.get(0);
        }
        throw new RuntimeException("未配置默认银行渠道：" + DEFAULT_CHANNEL);
    }

    public Long resolvePreferredAccountId(KingdeeBankChannel channel, Long requestAccountId) {
        if (channel != null && channel.getKingdeeAccountId() != null) {
            return channel.getKingdeeAccountId();
        }
        return requestAccountId;
    }

    public String resolveOrgCompanyCode(KingdeeBankChannel channel, KingdeeAccountContext account) {
        if (channel != null && StringUtils.hasText(channel.getKingdeeOrgCompanyCode())) {
            return channel.getKingdeeOrgCompanyCode().trim();
        }
        if (account != null && StringUtils.hasText(account.getOrgCompanyCode())) {
            return account.getOrgCompanyCode().trim();
        }
        return "111";
    }

    public String resolveBankAccount(KingdeeBankChannel channel) {
        if (channel == null) {
            throw new RuntimeException("银行渠道为空，无法解析金蝶银行科目");
        }
        if (!StringUtils.hasText(channel.getKingdeeBankAccount())) {
            throw new RuntimeException("渠道未配置金蝶银行科目：" + channel.getChannelKey());
        }
        return channel.getKingdeeBankAccount().trim();
    }

    public String resolveBankDimension(KingdeeBankChannel channel) {
        if (channel == null) {
            throw new RuntimeException("银行渠道为空，无法解析金蝶银行核算维度");
        }
        if (!StringUtils.hasText(channel.getKingdeeBankDimension())) {
            throw new RuntimeException("渠道未配置金蝶银行核算维度：" + channel.getChannelKey());
        }
        return channel.getKingdeeBankDimension().trim();
    }

    public String bankLabel(KingdeeBankChannel channel) {
        if (channel != null && StringUtils.hasText(channel.getBankName())) {
            return channel.getBankName().trim();
        }
        return "银行";
    }

    public String resolveBankAccountForDetail(KingdeeBankVoucherDetail detail) {
        return resolveBankAccount(resolveForDetail(detail));
    }

    public String resolveBankDimensionForDetail(KingdeeBankVoucherDetail detail) {
        return resolveBankDimension(resolveForDetail(detail));
    }
}
