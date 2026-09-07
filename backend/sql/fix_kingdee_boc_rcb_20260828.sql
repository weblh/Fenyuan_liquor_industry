-- 金蝶银行流水：中行纠正 + 农商行渠道 + 扫码免手续费标记（已有库执行）2026-08-28

-- 渠道表增加扫码免手续费标记（若列已存在可忽略本句报错后继续）
ALTER TABLE kingdee_bank_channel
    ADD COLUMN qr_fee_exempt INT DEFAULT 0 COMMENT '扫码收款免手续费 1=免 0=按FEE/费率拆分';

-- 纠正：719878715101 实为中行（非建行），模板 BOC_DETAIL
UPDATE kingdee_bank_channel
SET channel_key = 'FENYUAN_BOC',
    bank_code = 'BOC',
    bank_name = '中国银行',
    excel_template = 'BOC_DETAIL',
    qr_fee_exempt = 0,
    remark = '中国银行广州番禺祈福支行 719878715101；通联/美团扫码按摘要FEE拆分',
    update_time = CURRENT_TIMESTAMP
WHERE channel_key IN ('FENYUAN_CCB', 'FENYUAN_BOC')
   OR account_no = '719878715101';

-- 历史明细渠道键同步
UPDATE kingdee_bank_voucher_detail
SET channel_key = 'FENYUAN_BOC',
    bank_code = 'BOC',
    excel_template = 'BOC_DETAIL'
WHERE channel_key = 'FENYUAN_CCB';

-- 农商行渠道（微信收款免手续费）
INSERT INTO kingdee_bank_channel (
    channel_key, company_code, company_name, bank_code, bank_name,
    account_no, account_name, excel_template, kingdee_account_id,
    kingdee_org_company_code, kingdee_bank_account, kingdee_bank_dimension,
    qr_fee_exempt, sort_order, enabled, remark
)
SELECT
    'FENYUAN_RCB', '111', '广东汾源酒业', 'RCB', '农村商业银行',
    '00561580000000717', '广东汾源酒业有限公司', 'RCB_RECEIPT', 1,
    '111', '1002.31', 'NS001',
    1, 2, 1, '广州农商行化龙支行 00561580000000717（科目1002.31/维度NS001）；微信收款免手续费'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM kingdee_bank_channel WHERE channel_key = 'FENYUAN_RCB');
