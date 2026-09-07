package com.fenyuan.liquor.modules.kingdee.service.classifier;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 银行流水金额辅助：付款手续费为独立流水；扫码收款手续费嵌在摘要 FEE / 通联 SQEEFF 中。
 */
public final class BankVoucherFeeAmounts {

    /**
     * 摘要中的手续费：FEE10.8 / FEE:10.80 / FEE=10.8 / SQEEFF230.85（通联）/
     * 通联简写 TL...7F0.48 / 手续费10.8 / 手续费：10.80
     */
    private static final Pattern FEE_IN_TEXT = Pattern.compile(
            "(?i)(?:SQEEFF|7F|FEE|手续费|平台手续费|交易手续费)\\s*[=:：]?\\s*([0-9]+(?:\\.[0-9]+)?)");

    private static final Pattern GROSS_IN_TEXT = Pattern.compile(
            "(?:实付|实收|交易金额|订单金额|支付金额|总额)[:：]?\\s*([0-9,]+(?:\\.[0-9]+)?)");

    private BankVoucherFeeAmounts() {
    }

    /**
     * 从摘要/备注/用途解析嵌套手续费金额（扫码收款摘要中的 FEE）。
     */
    public static BigDecimal parseFeeFromText(String... texts) {
        if (texts == null) {
            return null;
        }
        BigDecimal found = null;
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Matcher m = FEE_IN_TEXT.matcher(text);
            while (m.find()) {
                try {
                    BigDecimal v = new BigDecimal(m.group(1).replace(",", ""))
                            .setScale(2, RoundingMode.HALF_UP);
                    if (v.compareTo(BigDecimal.ZERO) > 0) {
                        // 同一段文本多处 FEE 时取较大值（避免误取 0）
                        if (found == null || v.compareTo(found) > 0) {
                            found = v;
                        }
                    }
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        return found;
    }

    public static BigDecimal parseGrossFromText(String... texts) {
        if (texts == null) {
            return null;
        }
        BigDecimal best = null;
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Matcher m = GROSS_IN_TEXT.matcher(text);
            while (m.find()) {
                try {
                    BigDecimal v = new BigDecimal(m.group(1).replace(",", ""))
                            .setScale(2, RoundingMode.HALF_UP);
                    if (best == null || v.compareTo(best) > 0) {
                        best = v;
                    }
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        return best;
    }

    /**
     * 扫码收款：客户实付总额 = 银行到账净额 + 摘要 FEE；无 FEE 时再尝试总额字段 / 费率反推。
     */
    public static BigDecimal resolveQrGrossAmount(BigDecimal bankNet,
                                                   String summary, String remark, String purpose,
                                                   BigDecimal feeRateOrNull) {
        if (bankNet == null) {
            return null;
        }
        BigDecimal net = bankNet.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = parseFeeFromText(summary, remark, purpose);
        if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            return net.add(fee).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal labeledGross = parseGrossFromText(purpose, remark, summary);
        if (labeledGross != null && labeledGross.compareTo(net) > 0) {
            return labeledGross.setScale(2, RoundingMode.HALF_UP);
        }
        // feeRateOrNull<=0 表示免手续费渠道（如农商行微信），不再按费率反推
        if (feeRateOrNull == null || feeRateOrNull.compareTo(BigDecimal.ZERO) <= 0) {
            return net;
        }
        BigDecimal rate = feeRateOrNull;
        if (rate.compareTo(BigDecimal.ONE) >= 0) {
            return net;
        }
        return net.divide(BigDecimal.ONE.subtract(rate), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal resolveQrFeeAmount(BigDecimal bankNet, BigDecimal gross) {
        if (bankNet == null || gross == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = gross.subtract(bankNet).setScale(2, RoundingMode.HALF_UP);
        return fee.compareTo(BigDecimal.ZERO) > 0 ? fee : BigDecimal.ZERO;
    }

    /**
     * 查重候选金额：银行到账额；若扫码且摘要有 FEE，再追加「到账+手续费」客户实付额。
     */
    public static List<BigDecimal> candidateMatchAmounts(BigDecimal bankAmount,
                                                         boolean qrReceipt,
                                                         String summary, String remark, String purpose,
                                                         BigDecimal feeRateOrNull) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        List<BigDecimal> list = new ArrayList<>();
        if (bankAmount != null) {
            BigDecimal net = bankAmount.setScale(2, RoundingMode.HALF_UP);
            addAmount(list, keys, net);
            if (qrReceipt) {
                BigDecimal gross = resolveQrGrossAmount(net, summary, remark, purpose, feeRateOrNull);
                if (gross != null && gross.compareTo(net) > 0) {
                    addAmount(list, keys, gross);
                }
                BigDecimal fee = parseFeeFromText(summary, remark, purpose);
                if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
                    addAmount(list, keys, fee);
                }
            }
        }
        return list;
    }

    private static void addAmount(List<BigDecimal> list, LinkedHashSet<String> keys, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        String key = amount.toPlainString();
        if (keys.add(key)) {
            list.add(amount);
        }
    }
}
