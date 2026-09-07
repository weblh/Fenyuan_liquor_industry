-- 补全业务管理/财务管理父级菜单（子菜单 parent_id=26/32 但父节点缺失会导致侧边栏不显示）
USE fenyuan_liquor_industry;

INSERT IGNORE INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(26, 0, '业务管理', '/business', 'Layout', NULL, 0, 'ShopOutlined', 4, 1, 1, 0),
(27, 26, '在线销售管理', '/business/online-sale', 'business/onlineSale/index', 'business:onlineSale:list', 1, 'ShoppingCartOutlined', 4, 1, 1, 0),
(28, 26, '销售排名', '/business/sales-rank', 'business/salesRank/index', 'business:salesRank:list', 1, 'TrophyOutlined', 5, 1, 1, 0),
(29, 26, '汾源酒库存', '/business/inventory', 'business/inventory/index', 'business:inventory:list', 1, 'DatabaseOutlined', 6, 1, 1, 0),
(30, 26, '销售产品结构', '/business/product-structure', 'business/productStructure/index', 'business:productStructure:list', 1, 'PieChartOutlined', 7, 1, 1, 0),
(31, 26, '客户开发', '/business/customer-dev', 'business/customerDev/index', 'business:customerDev:list', 1, 'SolutionOutlined', 8, 1, 1, 0),
(32, 0, '财务管理', '/finance', 'Layout', NULL, 0, 'AccountBookOutlined', 5, 1, 1, 0),
(33, 32, '应收账款明细', '/finance/receivable', 'finance/receivable/index', 'finance:receivable:list', 1, 'MoneyCollectOutlined', 1, 1, 1, 0),
(34, 27, '新增', NULL, NULL, 'business:onlineSale:add', 2, NULL, 1, 1, 1, 0),
(35, 27, '编辑', NULL, NULL, 'business:onlineSale:edit', 2, NULL, 2, 1, 1, 0),
(36, 27, '删除', NULL, NULL, 'business:onlineSale:delete', 2, NULL, 3, 1, 1, 0),
(37, 28, '新增', NULL, NULL, 'business:salesRank:add', 2, NULL, 1, 1, 1, 0),
(38, 28, '编辑', NULL, NULL, 'business:salesRank:edit', 2, NULL, 2, 1, 1, 0),
(39, 28, '删除', NULL, NULL, 'business:salesRank:delete', 2, NULL, 3, 1, 1, 0),
(40, 29, '新增', NULL, NULL, 'business:inventory:add', 2, NULL, 1, 1, 1, 0),
(41, 29, '编辑', NULL, NULL, 'business:inventory:edit', 2, NULL, 2, 1, 1, 0),
(42, 29, '删除', NULL, NULL, 'business:inventory:delete', 2, NULL, 3, 1, 1, 0),
(43, 30, '新增', NULL, NULL, 'business:productStructure:add', 2, NULL, 1, 1, 1, 0),
(44, 30, '编辑', NULL, NULL, 'business:productStructure:edit', 2, NULL, 2, 1, 1, 0),
(45, 30, '删除', NULL, NULL, 'business:productStructure:delete', 2, NULL, 3, 1, 1, 0),
(46, 31, '新增', NULL, NULL, 'business:customerDev:add', 2, NULL, 1, 1, 1, 0),
(47, 31, '编辑', NULL, NULL, 'business:customerDev:edit', 2, NULL, 2, 1, 1, 0),
(48, 31, '删除', NULL, NULL, 'business:customerDev:delete', 2, NULL, 3, 1, 1, 0),
(49, 33, '新增', NULL, NULL, 'finance:receivable:add', 2, NULL, 1, 1, 1, 0),
(50, 33, '编辑', NULL, NULL, 'finance:receivable:edit', 2, NULL, 2, 1, 1, 0),
(51, 33, '删除', NULL, NULL, 'finance:receivable:delete', 2, NULL, 3, 1, 1, 0);

-- 确保核心子菜单排序正确
UPDATE sys_menu SET sort = 1 WHERE id = 52;
UPDATE sys_menu SET sort = 2 WHERE id = 56;
UPDATE sys_menu SET sort = 3 WHERE id = 60;
UPDATE sys_menu SET sort = 4 WHERE id = 27;
UPDATE sys_menu SET sort = 5 WHERE id = 28;
UPDATE sys_menu SET sort = 6 WHERE id = 29;
UPDATE sys_menu SET sort = 7 WHERE id = 30;
UPDATE sys_menu SET sort = 8 WHERE id = 31;
UPDATE sys_menu SET sort = 1 WHERE id = 33;
UPDATE sys_menu SET sort = 2 WHERE id = 64;

-- 超级管理员角色授权全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE del_flag = 0;
