package com.fenyuan.liquor.modules.kingdee.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 导出用表头（单列名单值）。导入请走 {@link com.fenyuan.liquor.modules.kingdee.service.BankVoucherExcelReader}，
 * 勿在此用 @ExcelProperty 多值当“别名”——EasyExcel 会当成多级表头。
 */
@Data
public class KingdeeBankVoucherDetailExcel {

    @ExcelProperty("账号")
    @ColumnWidth(22)
    private String accountNo;

    @ExcelProperty("账户名称")
    @ColumnWidth(22)
    private String accountName;

    @ExcelProperty("交易时间")
    @ColumnWidth(18)
    private String tradeTime;

    @ExcelProperty("借方发生额/元(支取)")
    @ColumnWidth(16)
    private String debitAmount;

    @ExcelProperty("贷方发生额/元(收入)")
    @ColumnWidth(16)
    private String creditAmount;

    @ExcelProperty("余额")
    @ColumnWidth(14)
    private String balance;

    @ExcelProperty("币种")
    @ColumnWidth(10)
    private String currency;

    @ExcelProperty("对方户名")
    @ColumnWidth(24)
    private String counterpartyName;

    @ExcelProperty("对方账号")
    @ColumnWidth(22)
    private String counterpartyAccount;

    @ExcelProperty("对方开户机构")
    @ColumnWidth(24)
    private String counterpartyBank;

    @ExcelProperty("记账日期")
    @ColumnWidth(12)
    private String bookkeepingDate;

    @ExcelProperty("摘要")
    @ColumnWidth(12)
    private String summary;

    @ExcelProperty("备注")
    @ColumnWidth(28)
    private String remark;

    @ExcelProperty("账户明细编号-交易流水号")
    @ColumnWidth(28)
    private String tradeSerialNo;

    @ExcelProperty("企业流水号")
    @ColumnWidth(16)
    private String enterpriseSerialNo;

    @ExcelProperty("凭证种类")
    @ColumnWidth(14)
    private String voucherKind;

    @ExcelProperty("凭证号")
    @ColumnWidth(14)
    private String bankVoucherNo;

    /** 工行电子回单号等，不导出主表头时可空 */
    private String receiptNo;

    /** 用途 */
    private String purpose;

    /** 原始方向：借/贷、往账/来账 */
    private String directionFlag;

    /** 原始单金额（工行/中行） */
    private String rawAmount;

    @ExcelProperty("凭证类型")
    @ColumnWidth(14)
    private String voucherTypeName;

    @ExcelProperty("写入状态")
    @ColumnWidth(10)
    private String writeStatusLabel;

    @ExcelProperty("金蝶凭证号")
    @ColumnWidth(12)
    private String kingdeeVoucherNo;
}
