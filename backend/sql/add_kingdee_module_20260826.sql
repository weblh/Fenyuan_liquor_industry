-- 金蝶模块：账号密码 + 账套（已有库执行）
-- 2026-08-26

CREATE TABLE IF NOT EXISTS kingdee_credential (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(128) NOT NULL,
    kingdee_url   VARCHAR(512) NOT NULL,
    username      VARCHAR(128) NOT NULL,
    password      VARCHAR(256) NOT NULL,
    status        TINYINT DEFAULT 1,
    remark        VARCHAR(255),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kingdee_account_set (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    credential_id           BIGINT NOT NULL,
    account_name            VARCHAR(128) NOT NULL,
    db_id                   VARCHAR(64) NOT NULL,
    org_company_code        VARCHAR(64),
    use_org_code            VARCHAR(64),
    default_form_id         VARCHAR(128) DEFAULT 'GL_VOUCHER',
    default_accounting_period VARCHAR(32),
    bank_account_no         VARCHAR(64),
    is_default              TINYINT DEFAULT 0,
    status                  TINYINT DEFAULT 1,
    description             VARCHAR(255),
    create_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 68, 0, '金蝶', '/kingdee', 'Layout', NULL, 0, 'CloudOutlined', 6, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 68);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 69, 68, '账号密码', '/kingdee/credential', 'kingdee/credential/index', 'kingdee:credential:list', 1, 'KeyOutlined', 1, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 69);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 70, 68, '账套', '/kingdee/account-set', 'kingdee/accountSet/index', 'kingdee:accountSet:list', 1, 'DatabaseOutlined', 2, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 70);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 71, 69, '新增', NULL, NULL, 'kingdee:credential:add', 2, NULL, 1, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 71);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 72, 69, '编辑', NULL, NULL, 'kingdee:credential:edit', 2, NULL, 2, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 72);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 73, 69, '删除', NULL, NULL, 'kingdee:credential:delete', 2, NULL, 3, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 73);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 74, 70, '新增', NULL, NULL, 'kingdee:accountSet:add', 2, NULL, 1, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 74);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 75, 70, '编辑', NULL, NULL, 'kingdee:accountSet:edit', 2, NULL, 2, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 75);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag)
SELECT 76, 70, '删除', NULL, NULL, 'kingdee:accountSet:delete', 2, NULL, 3, 1, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 76);

-- 管理员角色授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 68 AND 76
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = sys_menu.id);

-- 初始数据
INSERT INTO kingdee_credential (id, name, kingdee_url, username, password, status, remark)
SELECT 1, '黄增峰', 'https://dichanerp.huaxianggroup.cn/k3cloud/', '黄增峰', 'zf13643000166', 1, '汾源酒业金蝶登录账号' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM kingdee_credential WHERE id = 1);

INSERT INTO kingdee_account_set (id, credential_id, account_name, db_id, org_company_code, use_org_code, default_form_id, default_accounting_period, bank_account_no, is_default, status, description)
SELECT 1, 1, '广东汾源酒业有限公司', '61cbb7b2d5e132', '111', '111', 'GL_VOUCHER', '202607', '719878715101', 1, 1, '组织111 广东汾源酒业有限公司，账簿编码111' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM kingdee_account_set WHERE id = 1);
