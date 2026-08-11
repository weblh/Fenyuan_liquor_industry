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
(27, 26, '在线销售管理', '/business/online-sale', 'business/onlineSale/index', 'business:onlineSale:list', 1, 'ShoppingCartOutlined', 1, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(28, 26, '销售排名', '/business/sales-rank', 'business/salesRank/index', 'business:salesRank:list', 1, 'TrophyOutlined', 2, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(29, 26, '汾源酒库存', '/business/inventory', 'business/inventory/index', 'business:inventory:list', 1, 'DatabaseOutlined', 3, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(30, 26, '销售产品结构', '/business/product-structure', 'business/productStructure/index', 'business:productStructure:list', 1, 'PieChartOutlined', 4, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(31, 26, '客户开发', '/business/customer-dev', 'business/customerDev/index', 'business:customerDev:list', 1, 'SolutionOutlined', 5, 1, 1, 0);

-- 财务管理
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(32, 0, '财务管理', '/finance', 'Layout', NULL, 0, 'AccountBookOutlined', 5, 1, 1, 0);
INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(33, 32, '应收账款明细', '/finance/receivable', 'finance/receivable/index', 'finance:receivable:list', 1, 'MoneyCollectOutlined', 1, 1, 1, 0);

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

-- 业务示例数据
INSERT INTO biz_online_sale (id, sale_amount, ship_amount, payment_amount, period_name, del_flag) VALUES
(1, 1280000.00, 980000.00, 860000.00, '2026年1月', 0);
INSERT INTO biz_online_sale (id, sale_amount, ship_amount, payment_amount, period_name, del_flag) VALUES
(2, 1560000.00, 1320000.00, 1100000.00, '2026年2月', 0);

INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag) VALUES
(1, '太原经销商', 520000.00, 28.50, 1, 0);
INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag) VALUES
(2, '大同经销商', 410000.00, 22.40, -1, 0);
INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag) VALUES
(3, '临汾经销商', 380000.00, 20.80, 1, 0);

INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag) VALUES
(1, '汾源原浆', '500ml*6', 1200.00, 360000.00, '总库', 0);
INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag) VALUES
(2, '汾源陈酿', '42度 500ml', 800.00, 240000.00, '一号库', 0);
INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag) VALUES
(3, '汾源礼盒', '两瓶装', 350.00, 175000.00, '二号库', 0);

INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag) VALUES
(1, '原浆系列', 4500.00, 35.00, '经销商', 0);
INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag) VALUES
(2, '陈酿系列', 3200.00, 25.00, '商超', 0);
INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag) VALUES
(3, '礼盒系列', 2100.00, 16.50, '团购', 0);

INSERT INTO biz_customer_dev (id, name, amount, remark, del_flag) VALUES
(1, '晋中商贸有限公司', 86000.00, '新开拓区域经销', 0);
INSERT INTO biz_customer_dev (id, name, amount, remark, del_flag) VALUES
(2, '吕梁烟酒行', 42000.00, '首单合作', 0);

INSERT INTO fin_receivable (id, name, amount, remark, del_flag) VALUES
(1, '太原经销商', 156000.00, '账期30天', 0);
INSERT INTO fin_receivable (id, name, amount, remark, del_flag) VALUES
(2, '大同经销商', 98000.00, '部分回款', 0);
