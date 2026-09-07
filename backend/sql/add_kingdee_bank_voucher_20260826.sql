-- 银行流水凭证模块（已有库执行）2026-08-26

CREATE TABLE IF NOT EXISTS kingdee_bank_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_key VARCHAR(64) NOT NULL,
    company_code VARCHAR(32) NOT NULL,
    company_name VARCHAR(64) NOT NULL,
    bank_code VARCHAR(16) NOT NULL,
    bank_name VARCHAR(32) NOT NULL,
    account_no VARCHAR(64),
    account_name VARCHAR(128),
    excel_template VARCHAR(32) NOT NULL,
    kingdee_account_id BIGINT,
    kingdee_org_company_code VARCHAR(64),
    kingdee_bank_account VARCHAR(64),
    kingdee_bank_dimension VARCHAR(64),
    sort_order INT DEFAULT 0,
    enabled INT DEFAULT 1,
    remark VARCHAR(256),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_kingdee_bank_channel_key UNIQUE (channel_key)
);

CREATE TABLE IF NOT EXISTS kingdee_bank_voucher_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_key VARCHAR(64),
    company_code VARCHAR(32),
    bank_code VARCHAR(16),
    excel_template VARCHAR(32),
    source_file_name VARCHAR(256),
    receipt_no VARCHAR(128),
    purpose VARCHAR(256),
    direction_flag VARCHAR(16),
    raw_amount DECIMAL(18,2),
    extra_json LONGTEXT,
    account_no VARCHAR(64),
    account_name VARCHAR(128),
    trade_time VARCHAR(32),
    debit_amount DECIMAL(18,2),
    credit_amount DECIMAL(18,2),
    balance DECIMAL(18,2),
    currency VARCHAR(32),
    counterparty_name VARCHAR(256),
    counterparty_account VARCHAR(128),
    counterparty_bank VARCHAR(256),
    bookkeeping_date VARCHAR(16),
    summary VARCHAR(128),
    remark VARCHAR(512),
    trade_serial_no VARCHAR(128),
    enterprise_serial_no VARCHAR(128),
    voucher_kind VARCHAR(64),
    bank_voucher_no VARCHAR(64),
    voucher_type VARCHAR(64),
    voucher_type_name VARCHAR(64),
    write_status INT DEFAULT 0,
    kingdee_voucher_id VARCHAR(64),
    kingdee_voucher_no VARCHAR(64),
    kingdee_voucher_word VARCHAR(32),
    platform_write_time TIMESTAMP,
    write_error VARCHAR(500),
    kingdee_account_id BIGINT,
    kingdee_exist_status INT DEFAULT 0,
    kingdee_exist_voucher_word VARCHAR(32),
    kingdee_exist_voucher_no VARCHAR(64),
    kingdee_exist_checked_at TIMESTAMP,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 77, 68, '银行流水凭证', '/kingdee/bank-voucher', 'kingdee/bankVoucher/index', 'kingdee:bankVoucher:list', 1, 'BankOutlined', 3, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 77);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 78, 77, '导入流水', NULL, NULL, 'kingdee:bankVoucher:import', 2, NULL, 1, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 78);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 79, 77, '写入金蝶', NULL, NULL, 'kingdee:bankVoucher:write', 2, NULL, 2, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 79);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (77, 78, 79)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.id);

INSERT INTO kingdee_bank_channel (id, channel_key, company_code, company_name, bank_code, bank_name, account_no, account_name, excel_template, kingdee_account_id, kingdee_org_company_code, kingdee_bank_account, kingdee_bank_dimension, sort_order, enabled, remark)
SELECT 1, 'FENYUAN_BOC', '111', '广东汾源酒业', 'BOC', '中国银行', '719878715101', '广东汾源酒业有限公司', 'BOC_DETAIL', 1, '111', '1002.15', '007', 1, 1, '中国银行广州番禺祈福支行 719878715101（科目1002.15/维度007）' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM kingdee_bank_channel WHERE channel_key IN ('FENYUAN_BOC', 'FENYUAN_CCB'));
