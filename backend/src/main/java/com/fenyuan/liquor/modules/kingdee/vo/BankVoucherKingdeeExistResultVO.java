package com.fenyuan.liquor.modules.kingdee.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BankVoucherKingdeeExistResultVO {

    private int total;
    private int existsCount;
    private int notExistsCount;
    private int uncheckedCount;
    private List<String> periodsQueried = new ArrayList<>();
    private int kingdeeEntryCount;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long id;
        private String tradeSerialNo;
        private String tradeTime;
        private String bookkeepingDate;
        private String amountSide;
        private String amount;
        /** 1金蝶已存在 2不存在 0未检查 */
        private Integer kingdeeExistStatus;
        private String kingdeeExistVoucherWord;
        private String kingdeeExistVoucherNo;
        private String message;
    }
}
