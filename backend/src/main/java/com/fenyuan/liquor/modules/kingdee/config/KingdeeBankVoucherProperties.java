package com.fenyuan.liquor.modules.kingdee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "kingdee.voucher.bank-detail")
public class KingdeeBankVoucherProperties {

    private String formId = "GL_VOUCHER";
    private String voucherGroup = "银";
    private String voucherGroupNumber = "PZZ2";
    private String bankAccount = "1002.15";
    private String bankDimensionNumber = "007";
    private String bankDimensionKey = "FDETAILID__FFLEX14";
    private String supplierDimensionKey = "FDETAILID__FFLEX4";
    private String employeeDimensionKey = "FDETAILID__FFLEX7";
    private String customerDimensionKey = "FDETAILID__FFLEX6";
    private String feeAccount = "6603.04";
    /**
     * 扫码收款（银联条码等）平台手续费率。银行到账为净额时，按净额/(1-rate) 反推客户实付总额。
     * 例：3589.2 / (1-0.003) = 3600，手续费 10.8。
     */
    private String qrFeeRate = "0.003";
    /**
     * 扫码收款对方户名为空（或仅为银行清算户）时，用于匹配金蝶客户档案的默认客户名称。
     */
    private String qrCustomerName = "扫码收款";
    private String payableAccount = "2202.02";
    private String expenseAccount = "2241.03";
    private String loanAccount = "2203.02";
    private String receivableAccount = "1122.01";
    private String personalTaxAccount = "2221.13";
    private String stampTaxAccount = "2221.08";
    private String vatTaxAccount = "2221.16";
    private String urbanTaxAccount = "2221.07";
    private String educationSurchargeAccount = "2221.11";
    private String localEducationSurchargeAccount = "2221.15";
    private String interestIncomeAccount = "6603.02";
    private String discountInterestAccount = "6603.07";
    private String notesReceivableBaAccount = "1121.02";
    private String notesReceivableFinAccount = "1121.04";
    private String salaryPayableAccount = "2211.06";
    private String otherReceivableAccount = "1221.12";
    private String currencyId = "PRE001";
    private String exchangeRateType = "HLTX01_SYS";
    private String exchangeRate = "1";

    public String resolveVoucherGroupNumber(String queried) {
        // 优先使用金蝶档案查到的真实编码；配置值仅作兜底（PZZ2 在部分账套可能不存在）
        if (StringUtils.hasText(queried)) {
            return queried.trim();
        }
        if (StringUtils.hasText(voucherGroupNumber)) {
            return voucherGroupNumber.trim();
        }
        return "PRE001";
    }
}
