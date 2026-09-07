package com.fenyuan.liquor.modules.kingdee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kingdee_bank_channel")
public class KingdeeBankChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** FENGCHI_CCB 等，前端 Tab 过滤键 */
    private String channelKey;

    private String companyCode;

    private String companyName;

    private String bankCode;

    private String bankName;

    private String accountNo;

    private String accountName;

    /** CCB_DETAIL / ICBC_RECEIPT / ICBC_HISTORY / BOC_DETAIL / RCB_RECEIPT */
    private String excelTemplate;

    private Long kingdeeAccountId;

    /** 金蝶账簿编码 FAccountBookID，如 002 丰驰、003 昌泽、115 耀通（不是组织编码 110/113） */
    private String kingdeeOrgCompanyCode;

    private String kingdeeBankAccount;

    private String kingdeeBankDimension;

    /**
     * 扫码收款是否免手续费（1=免，如农商行微信；0=按摘要FEE/费率拆分，如中行通联）。
     */
    private Integer qrFeeExempt;

    private Integer sortOrder;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
