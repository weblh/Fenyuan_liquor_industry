package com.fenyuan.liquor.modules.kingdee.vo;

import java.util.ArrayList;
import java.util.List;

public class KingdeeVoucherSaveResultVO {

    private String voucherId;
    private String voucherNumber;
    private String explanation;
    private String voucherGroup;
    private String savedMode;
    private Integer itemCount;
    private Integer successCount;
    private Integer failCount;
    /** 因类型不可自动匹配科目而跳过写入的条数 */
    private Integer skippedCount;
    private String message;
    /** 写入失败明细（按发票/凭证），便于前端完整展示原因 */
    private List<FailItem> failItems = new ArrayList<>();

    public static class FailItem {
        private Long id;
        private String tradeSerialNo;
        private String bookkeepingDate;
        private String amount;
        private String message;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTradeSerialNo() {
            return tradeSerialNo;
        }

        public void setTradeSerialNo(String tradeSerialNo) {
            this.tradeSerialNo = tradeSerialNo;
        }

        public String getBookkeepingDate() {
            return bookkeepingDate;
        }

        public void setBookkeepingDate(String bookkeepingDate) {
            this.bookkeepingDate = bookkeepingDate;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getVoucherGroup() {
        return voucherGroup;
    }

    public void setVoucherGroup(String voucherGroup) {
        this.voucherGroup = voucherGroup;
    }

    public String getSavedMode() {
        return savedMode;
    }

    public void setSavedMode(String savedMode) {
        this.savedMode = savedMode;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FailItem> getFailItems() {
        return failItems;
    }

    public void setFailItems(List<FailItem> failItems) {
        this.failItems = failItems != null ? failItems : new ArrayList<>();
    }
}
