-- 初始化数据：汾源酒业经营体

-- 部门
INSERT INTO sys_dept (id, parent_id, name, code, leader, phone, sort, status, del_flag) VALUES
(1, 0, '汾源酒业集团', 'FYJT', '张总', '0350-1000000', 1, 1, 0);
INSERT INTO sys_dept (id, parent_id, name, code, leader, phone, sort, status, del_flag) VALUES
(2, 1, '总经办', 'ZJB', '李主任', '0350-1000001', 1, 1, 0);
INSERT INTO sys_dept (id, parent_id, name, code, leader, phone, sort, status, del_flag) VALUES
(3, 1, '销售部', 'XSB', '王经理', '0350-1000002', 2, 1, 0);
INSERT INTO sys_dept (id, parent_id, name, code, leader, phone, sort, status, del_flag) VALUES
(4, 1, '生产部', 'SCB', '赵经理', '0350-1000003', 3, 1, 0);
INSERT INTO sys_dept (id, parent_id, name, code, leader, phone, sort, status, del_flag) VALUES
(5, 1, '财务部', 'CWB', '钱经理', '0350-1000004', 4, 1, 0);

-- 用户 admin / admin123 (BCrypt)
INSERT INTO sys_user (id, username, password, nickname, real_name, email, phone, dept_id, status, is_admin, del_flag) VALUES
(1, 'admin', '$2a$10$FvqDf2750EP972HV76MnOOb44BqPE9E0Y2NqguqnTdO6heekJaBxO', '超级管理员', '系统管理员', 'admin@fenyuan.com', '13800138000', 1, 1, 1, 0);

-- 角色
INSERT INTO sys_role (id, name, code, description, sort, status, data_scope, del_flag) VALUES
(1, '超级管理员', 'ROLE_ADMIN', '系统最高权限角色', 1, 1, 1, 0);
INSERT INTO sys_role (id, name, code, description, sort, status, data_scope, del_flag) VALUES
(2, '普通用户', 'ROLE_USER', '普通用户角色', 2, 1, 3, 0);

-- 菜单：系统管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(1, 0, '系统管理', '/system', 'Layout', NULL, 0, 'SettingOutlined', 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(2, 1, '用户管理', '/system/user', 'system/user/index', 'system:user:list', 1, 'UserOutlined', 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(3, 1, '角色管理', '/system/role', 'system/role/index', 'system:role:list', 1, 'TeamOutlined', 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(4, 1, '菜单管理', '/system/menu', 'system/menu/index', 'system:menu:list', 1, 'MenuOutlined', 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(5, 1, '部门管理', '/system/dept', 'system/dept/index', 'system:dept:list', 1, 'ApartmentOutlined', 4, 1, 1, 0);

-- 日志管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(6, 0, '日志管理', '/log', 'Layout', NULL, 0, 'FileTextOutlined', 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(7, 6, '操作日志', '/log/oper', 'log/oper/index', 'log:oper:list', 1, 'FileSearchOutlined', 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(8, 6, '登录日志', '/log/login', 'log/login/index', 'log:login:list', 1, 'LoginOutlined', 2, 1, 1, 0);

-- 系统配置
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(9, 0, '系统配置', '/settings', 'Layout', NULL, 0, 'ToolOutlined', 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(10, 9, '参数配置', '/settings/config', 'settings/config/index', 'system:config:list', 1, 'SettingOutlined', 1, 1, 1, 0);

-- 用户管理按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(11, 2, '新增用户', NULL, NULL, 'system:user:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(12, 2, '编辑用户', NULL, NULL, 'system:user:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(13, 2, '删除用户', NULL, NULL, 'system:user:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(14, 2, '重置密码', NULL, NULL, 'system:user:resetPwd', 2, NULL, 4, 1, 1, 0);

-- 角色管理按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(15, 3, '新增角色', NULL, NULL, 'system:role:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(16, 3, '编辑角色', NULL, NULL, 'system:role:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(17, 3, '删除角色', NULL, NULL, 'system:role:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(18, 3, '分配权限', NULL, NULL, 'system:role:permission', 2, NULL, 4, 1, 1, 0);

-- 菜单管理按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(19, 4, '新增菜单', NULL, NULL, 'system:menu:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(20, 4, '编辑菜单', NULL, NULL, 'system:menu:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(21, 4, '删除菜单', NULL, NULL, 'system:menu:delete', 2, NULL, 3, 1, 1, 0);

-- 部门管理按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(22, 5, '新增部门', NULL, NULL, 'system:dept:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(23, 5, '编辑部门', NULL, NULL, 'system:dept:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(24, 5, '删除部门', NULL, NULL, 'system:dept:delete', 2, NULL, 3, 1, 1, 0);

-- 配置按钮
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(25, 10, '修改配置', NULL, NULL, 'system:config:edit', 2, NULL, 1, 1, 1, 0);

-- 业务管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(26, 0, '业务管理', '/business', 'Layout', NULL, 0, 'ShopOutlined', 4, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(52, 26, '客户维护监管', '/business/customer-maintain', 'business/customerMaintain/index', 'business:customerMaintain:list', 1, 'CustomerServiceOutlined', 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(56, 26, '酒类价格对比', '/business/price-compare', 'business/priceCompare/index', 'business:priceCompare:list', 1, 'FundOutlined', 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(60, 26, '异地销售统计', '/business/offsite-sales', 'business/offsiteSales/index', 'business:offsiteSale:list', 1, 'EnvironmentOutlined', 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(27, 26, '在线销售管理', '/business/online-sale', 'business/onlineSale/index', 'business:onlineSale:list', 1, 'ShoppingCartOutlined', 4, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(28, 26, '销售排名', '/business/sales-rank', 'business/salesRank/index', 'business:salesRank:list', 1, 'TrophyOutlined', 5, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(29, 26, '汾源酒库存', '/business/inventory', 'business/inventory/index', 'business:inventory:list', 1, 'DatabaseOutlined', 6, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(30, 26, '销售产品结构', '/business/product-structure', 'business/productStructure/index', 'business:productStructure:list', 1, 'PieChartOutlined', 7, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(31, 26, '客户开发', '/business/customer-dev', 'business/customerDev/index', 'business:customerDev:list', 1, 'SolutionOutlined', 8, 1, 1, 0);

-- 财务管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(32, 0, '财务管理', '/finance', 'Layout', NULL, 0, 'AccountBookOutlined', 5, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(33, 32, '应收账款明细', '/finance/receivable', 'finance/receivable/index', 'finance:receivable:list', 1, 'MoneyCollectOutlined', 1, 0, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(64, 32, '凭证记录', '/finance/kingdee-voucher', 'finance/kingdeeVoucher/index', 'finance:kingdeeVoucher:list', 1, 'FileSyncOutlined', 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(80, 26, '管家婆同步', '/business/cmcloud-sync', 'business/cmcloudSync/index', 'business:cmcloud:sync', 1, 'CloudSyncOutlined', 9, 1, 1, 0);

-- 业务/财务按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(34, 27, '新增', NULL, NULL, 'business:onlineSale:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(35, 27, '编辑', NULL, NULL, 'business:onlineSale:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(36, 27, '删除', NULL, NULL, 'business:onlineSale:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(37, 28, '新增', NULL, NULL, 'business:salesRank:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(38, 28, '编辑', NULL, NULL, 'business:salesRank:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(39, 28, '删除', NULL, NULL, 'business:salesRank:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(40, 29, '新增', NULL, NULL, 'business:inventory:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(41, 29, '编辑', NULL, NULL, 'business:inventory:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(42, 29, '删除', NULL, NULL, 'business:inventory:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(43, 30, '新增', NULL, NULL, 'business:productStructure:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(44, 30, '编辑', NULL, NULL, 'business:productStructure:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(45, 30, '删除', NULL, NULL, 'business:productStructure:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(46, 31, '新增', NULL, NULL, 'business:customerDev:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(47, 31, '编辑', NULL, NULL, 'business:customerDev:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(48, 31, '删除', NULL, NULL, 'business:customerDev:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(49, 33, '新增', NULL, NULL, 'finance:receivable:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(50, 33, '编辑', NULL, NULL, 'finance:receivable:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(51, 33, '删除', NULL, NULL, 'finance:receivable:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(53, 52, '新增', NULL, NULL, 'business:customerMaintain:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(54, 52, '编辑', NULL, NULL, 'business:customerMaintain:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(55, 52, '删除', NULL, NULL, 'business:customerMaintain:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(57, 56, '新增', NULL, NULL, 'business:priceCompare:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(58, 56, '编辑', NULL, NULL, 'business:priceCompare:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(59, 56, '删除', NULL, NULL, 'business:priceCompare:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(61, 60, '新增', NULL, NULL, 'business:offsiteSale:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(62, 60, '编辑', NULL, NULL, 'business:offsiteSale:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(63, 60, '删除', NULL, NULL, 'business:offsiteSale:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(65, 64, '新增', NULL, NULL, 'finance:kingdeeVoucher:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(66, 64, '编辑', NULL, NULL, 'finance:kingdeeVoucher:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(67, 64, '删除', NULL, NULL, 'finance:kingdeeVoucher:delete', 2, NULL, 3, 1, 1, 0);

-- 金蝶管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(68, 0, '金蝶', '/kingdee', 'Layout', NULL, 0, 'CloudOutlined', 6, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(69, 68, '账号密码', '/kingdee/credential', 'kingdee/credential/index', 'kingdee:credential:list', 1, 'KeyOutlined', 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(70, 68, '账套', '/kingdee/account-set', 'kingdee/accountSet/index', 'kingdee:accountSet:list', 1, 'DatabaseOutlined', 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(71, 69, '新增', NULL, NULL, 'kingdee:credential:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(72, 69, '编辑', NULL, NULL, 'kingdee:credential:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(73, 69, '删除', NULL, NULL, 'kingdee:credential:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(74, 70, '新增', NULL, NULL, 'kingdee:accountSet:add', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(75, 70, '编辑', NULL, NULL, 'kingdee:accountSet:edit', 2, NULL, 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(76, 70, '删除', NULL, NULL, 'kingdee:accountSet:delete', 2, NULL, 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(77, 68, '银行流水凭证', '/kingdee/bank-voucher', 'kingdee/bankVoucher/index', 'kingdee:bankVoucher:list', 1, 'BankOutlined', 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(78, 77, '导入流水', NULL, NULL, 'kingdee:bankVoucher:import', 2, NULL, 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(79, 77, '写入金蝶', NULL, NULL, 'kingdee:bankVoucher:write', 2, NULL, 2, 1, 1, 0);

-- admin -> ROLE_ADMIN
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ROLE_ADMIN 全部菜单
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu WHERE del_flag = 0;

-- 系统配置
INSERT INTO sys_config (id, name, config_key, config_value, type, group_name, status, is_system, del_flag) VALUES
(1, '系统名称', 'system.name', '汾源酒业经营体管理系统', 'string', 'system', 1, 1, 0);
INSERT INTO sys_config (id, name, config_key, config_value, type, group_name, status, is_system, del_flag) VALUES
(2, '系统Logo', 'system.logo', '/logo.png', 'string', 'system', 1, 1, 0);
INSERT INTO sys_config (id, name, config_key, config_value, type, group_name, status, is_system, del_flag) VALUES
(3, '系统版本', 'system.version', 'v1.0.0', 'string', 'system', 1, 1, 0);

-- 业务数据由管家婆 ERP 同步，不预置演示数据

INSERT INTO fin_kingdee_voucher (id, bank_flow_no, counterparty, amount, flow_date, sync_status, voucher_no, sync_time, remark, del_flag) VALUES
(1, 'BK20260825001', '太原经销商', 50000.00, '2026-08-24', 1, 'KD-PZ-20260824-001', '2026-08-24 18:30:00', '已根据银行流水写入金蝶', 0);
INSERT INTO fin_kingdee_voucher (id, bank_flow_no, counterparty, amount, flow_date, sync_status, voucher_no, sync_time, remark, del_flag) VALUES
(2, 'BK20260825002', '大同经销商', 28000.00, '2026-08-25', 0, NULL, NULL, '待同步金蝶凭证', 0);

-- 金蝶账号密码（黄增峰 / zf13643000166）
INSERT INTO kingdee_credential (id, name, kingdee_url, username, password, status, remark) VALUES
(1, '黄增峰', 'https://dichanerp.huaxianggroup.cn/k3cloud/', '黄增峰', 'zf13643000166', 1, '汾源酒业金蝶登录账号');

-- 金蝶账套（111 广东汾源酒业有限公司，建行 719878715101）
INSERT INTO kingdee_account_set (id, credential_id, account_name, db_id, org_company_code, use_org_code, default_form_id, default_accounting_period, bank_account_no, is_default, status, description) VALUES
(1, 1, '广东汾源酒业有限公司', '61cbb7b2d5e132', '111', '111', 'GL_VOUCHER', '202607', '719878715101', 1, 1, '组织111 广东汾源酒业有限公司，账簿编码111');

-- 金蝶银行流水渠道（建行 719878715101）
INSERT INTO kingdee_bank_channel (id, channel_key, company_code, company_name, bank_code, bank_name, account_no, account_name, excel_template, kingdee_account_id, kingdee_org_company_code, kingdee_bank_account, kingdee_bank_dimension, qr_fee_exempt, sort_order, enabled, remark) VALUES
(1, 'FENYUAN_BOC', '111', '广东汾源酒业', 'BOC', '中国银行', '719878715101', '广东汾源酒业有限公司', 'BOC_DETAIL', 1, '111', '1002.15', '007', 0, 1, 1, '中国银行广州番禺祈福支行 719878715101（科目1002.15/维度007）；通联/美团扫码按摘要FEE拆分'),
(2, 'FENYUAN_RCB', '111', '广东汾源酒业', 'RCB', '农村商业银行', '00561580000000717', '广东汾源酒业有限公司', 'RCB_RECEIPT', 1, '111', '1002.31', 'NS001', 1, 2, 1, '广州农商行化龙支行 00561580000000717（科目1002.31/维度NS001）；微信收款免手续费');
