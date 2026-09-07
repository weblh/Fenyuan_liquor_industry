package com.fenyuan.liquor.modules.kingdee.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class BankVoucherTypeInferResultVO {

    private int total;
    private int matched;
    private int unmatched;
    private int updated;
    private int skippedWritten;
    /** 匹配成功且属于可写入类型的条数 */
    private int writableMatched;
    /** 匹配成功但不可写入（社保/工资/贴现/其他）的条数 */
    private int nonWritableMatched;
    /** 类型可写但金蝶客户/员工/供应商档案缺失的条数 */
    private int masterDataBlocked;
    /** 金蝶未命中、仅本地按销方/票种识别类型的条数 */
    private int localIdentified;
    /** 会计期间列表，如 202606 */
    private List<String> periodsQueried = new ArrayList<>();
    /** 金蝶拉取到的分录条数 */
    private int kingdeeEntryCount;
    /** 未识别到类型的对方科目（代码 -> 名称/摘要样例） */
    private Map<String, String> unknownAccounts = new LinkedHashMap<>();
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long id;
        private String tradeSerialNo;
        private String bookkeepingDate;
        private String amountSide;
        private String amount;
        /** true=命中金蝶历史凭证；false=未命中（可能仍本地识别了类型） */
        private boolean matched;
        /** true=金蝶未命中，仅本地识别类型 */
        private boolean localOnly;
        private boolean writable;
        private String voucherType;
        private String voucherTypeName;
        private String previousType;
        private String matchedExplanation;
        private String matchedAccountCode;
        private String matchedAccountName;
        private String matchedVoucherWord;
        private String matchedVoucherNo;
        private String message;
    }
}
