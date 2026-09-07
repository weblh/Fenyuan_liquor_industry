package com.fenyuan.liquor.modules.kingdee.dto;

import lombok.Data;

import java.util.List;

@Data
public class KingdeeBankVoucherWriteRequest {
    private List<Long> ids;
    private Long accountId;
    /** 当前银行渠道（决定写入哪个账套组织） */
    private String channelKey;
}
