package com.fenyuan.liquor.modules.kingdee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kingdee_bank_voucher_detail")
public class KingdeeBankVoucherDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** FENGCHI_CCB 等渠道键 */
    private String channelKey;

    private String companyCode;

    private String bankCode;

    /** CCB_DETAIL / ICBC_RECEIPT / ICBC_HISTORY / BOC_DETAIL */
    private String excelTemplate;

    private String sourceFileName;

    private String receiptNo;

    private String purpose;

    /** 原始方向：借/贷、往账/来账 */
    private String directionFlag;

    private java.math.BigDecimal rawAmount;

    private String extraJson;

    private String accountNo;

    private String accountName;

    private String tradeTime;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;

    private BigDecimal balance;

    private String currency;

    private String counterpartyName;

    private String counterpartyAccount;

    private String counterpartyBank;

    private String bookkeepingDate;

    private String summary;

    private String remark;

    private String tradeSerialNo;

    private String enterpriseSerialNo;

    private String voucherKind;

    private String bankVoucherNo;

    /** 凭证业务类型编码，如 FEE / GOODS / FREIGHT */
    private String voucherType;

    private String voucherTypeName;

    /** 0未写入 1已写入 2失败 */
    private Integer writeStatus;

    private String kingdeeVoucherId;

    private String kingdeeVoucherNo;

    private String kingdeeVoucherWord;

    private LocalDateTime platformWriteTime;

    private String writeError;

    private Long kingdeeAccountId;

    /** 金蝶重复检查：0未检查 1已存在 2不存在 */
    private Integer kingdeeExistStatus;

    private String kingdeeExistVoucherWord;

    private String kingdeeExistVoucherNo;

    private LocalDateTime kingdeeExistCheckedAt;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
