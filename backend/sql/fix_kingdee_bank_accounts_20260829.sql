-- 纠正银行科目/核算维度（与金蝶账簿111实际科目一致）2026-08-29
-- 错误：农商行误配 1002.02（工行基本户9585），中行误配 1002.01
-- 正确：中行 1002.15 + 维度007；广州农商行 1002.31 + 维度NS001

UPDATE kingdee_bank_channel
SET kingdee_bank_account = '1002.15',
    kingdee_bank_dimension = '007',
    remark = '中国银行广州番禺祈福支行 719878715101（科目1002.15/维度007）；通联/美团扫码按摘要FEE拆分',
    update_time = CURRENT_TIMESTAMP
WHERE channel_key = 'FENYUAN_BOC';

UPDATE kingdee_bank_channel
SET kingdee_bank_account = '1002.31',
    kingdee_bank_dimension = 'NS001',
    remark = '广州农商行化龙支行 00561580000000717（科目1002.31/维度NS001）；微信收款免手续费',
    update_time = CURRENT_TIMESTAMP
WHERE channel_key = 'FENYUAN_RCB';
