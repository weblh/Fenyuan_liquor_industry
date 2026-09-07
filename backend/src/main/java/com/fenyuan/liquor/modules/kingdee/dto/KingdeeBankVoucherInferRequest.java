package com.fenyuan.liquor.modules.kingdee.dto;

import lombok.Data;

import java.util.List;

@Data
public class KingdeeBankVoucherInferRequest {

    /** 为空则对当前未写入明细全部反推 */
    private List<Long> ids;

    /** 金蝶账号，空则用渠道绑定账号或默认账号 */
    private Long accountId;

    /** 当前银行渠道（决定账套组织 + 银行科目） */
    private String channelKey;

    /** 额外指定会计期间（YYYYMM），不传则按明细记账日期自动取期间 */
    private List<String> accountingPeriods;

    /** true 时仅预览不落库 */
    private Boolean dryRun;
}
