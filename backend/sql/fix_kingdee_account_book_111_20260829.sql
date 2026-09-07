-- 修复：金蝶账簿编码误配为 11，实际为 111（组织/账簿同为 111）
-- 错误表现：写入时报「字段凭证字是必填项」（账簿无效时凭证字 FNumber 解析失败被清空）
UPDATE kingdee_account_set
SET org_company_code = '111',
    use_org_code = '111',
    description = '组织111 广东汾源酒业有限公司，账簿编码111'
WHERE org_company_code = '11' OR use_org_code = '11';

UPDATE kingdee_bank_channel
SET kingdee_org_company_code = '111'
WHERE kingdee_org_company_code = '11';
