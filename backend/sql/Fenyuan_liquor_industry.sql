-- ============================================================
-- 汾源酒业经营体：独立数据库（与 gongcheng_leasing 互不影响）
-- 数据库名：Fenyuan_liquor_industry
-- 账号示例：root / 123456
-- ============================================================

CREATE DATABASE IF NOT EXISTS Fenyuan_liquor_industry
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE Fenyuan_liquor_industry;

-- 部门
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT DEFAULT 0 COMMENT '父部门ID',
    name        VARCHAR(50) NOT NULL COMMENT '部门名称',
    code        VARCHAR(50) COMMENT '部门编码',
    leader      VARCHAR(50) COMMENT '负责人',
    phone       VARCHAR(20) COMMENT '联系电话',
    email       VARCHAR(100) COMMENT '邮箱',
    sort        INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志',
    remark      VARCHAR(255) COMMENT '备注',
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(50) NOT NULL COMMENT '用户名',
    password     VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    nickname     VARCHAR(50) COMMENT '昵称',
    real_name    VARCHAR(50) COMMENT '真实姓名',
    email        VARCHAR(100) COMMENT '邮箱',
    phone        VARCHAR(20) COMMENT '手机号',
    avatar       VARCHAR(255) COMMENT '头像',
    sex          TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    dept_id      BIGINT COMMENT '部门ID',
    status       TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_admin     TINYINT DEFAULT 0 COMMENT '是否管理员',
    login_ip     VARCHAR(50) COMMENT '最后登录IP',
    login_date   DATETIME COMMENT '最后登录时间',
    create_by    BIGINT COMMENT '创建人ID',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT COMMENT '更新人ID',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag     TINYINT DEFAULT 0 COMMENT '删除标志',
    remark       VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    name        VARCHAR(50) NOT NULL COMMENT '角色名称',
    code        VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    sort        INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    data_scope  TINYINT DEFAULT 1 COMMENT '数据范围',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志',
    remark      VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id   BIGINT DEFAULT 0 COMMENT '父菜单ID',
    name        VARCHAR(50) NOT NULL COMMENT '菜单名称',
    path        VARCHAR(200) COMMENT '路由路径',
    component   VARCHAR(200) COMMENT '组件路径',
    permission  VARCHAR(100) COMMENT '权限标识',
    type        TINYINT NOT NULL COMMENT '类型：0-目录，1-菜单，2-按钮',
    icon        VARCHAR(50) COMMENT '图标',
    sort        INT DEFAULT 0 COMMENT '排序',
    visible     TINYINT DEFAULT 1 COMMENT '是否可见',
    keep_alive  TINYINT DEFAULT 0 COMMENT '是否缓存',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志',
    remark      VARCHAR(255) COMMENT '备注',
    KEY idx_parent_id (parent_id),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id   BIGINT NOT NULL COMMENT '用户ID',
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    menu_id   BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    username    VARCHAR(50) COMMENT '操作用户名',
    user_id     BIGINT COMMENT '操作用户ID',
    module      VARCHAR(50) COMMENT '操作模块',
    operation   VARCHAR(200) COMMENT '操作描述',
    method      VARCHAR(200) COMMENT '请求方法名',
    params      TEXT COMMENT '请求参数',
    result      TEXT COMMENT '返回结果',
    ip          VARCHAR(50) COMMENT '请求IP',
    location    VARCHAR(100) COMMENT '操作地点',
    browser     VARCHAR(50) COMMENT '浏览器',
    os          VARCHAR(50) COMMENT '操作系统',
    status      TINYINT COMMENT '操作状态：0-失败，1-成功',
    error_msg   TEXT COMMENT '错误信息',
    time        BIGINT COMMENT '执行耗时(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    username    VARCHAR(50) COMMENT '用户名',
    user_id     BIGINT COMMENT '用户ID',
    ip          VARCHAR(50) COMMENT '登录IP',
    location    VARCHAR(100) COMMENT '登录地点',
    browser     VARCHAR(50) COMMENT '浏览器',
    os          VARCHAR(50) COMMENT '操作系统',
    status      TINYINT COMMENT '登录状态：0-失败，1-成功',
    msg         VARCHAR(255) COMMENT '提示信息',
    login_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_username (username),
    KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    name         VARCHAR(50) NOT NULL COMMENT '配置名称',
    config_key   VARCHAR(50) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    type         VARCHAR(20) DEFAULT 'string' COMMENT '配置类型',
    group_name   VARCHAR(50) COMMENT '配置分组',
    status       TINYINT DEFAULT 1 COMMENT '状态',
    is_system    TINYINT DEFAULT 0 COMMENT '是否系统内置',
    create_by    BIGINT COMMENT '创建人ID',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT COMMENT '更新人ID',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag     TINYINT DEFAULT 0 COMMENT '删除标志',
    remark       VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS biz_online_sale (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    sale_amount    DECIMAL(18, 2) DEFAULT 0 COMMENT '销售',
    ship_amount    DECIMAL(18, 2) DEFAULT 0 COMMENT '发货',
    payment_amount DECIMAL(18, 2) DEFAULT 0 COMMENT '回款',
    period_name    VARCHAR(50) COMMENT '期间',
    create_by      BIGINT COMMENT '创建人',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      BIGINT COMMENT '更新人',
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag       TINYINT DEFAULT 0 COMMENT '删除标志',
    remark         VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线销售管理';

CREATE TABLE IF NOT EXISTS biz_sales_rank (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    company_name  VARCHAR(100) NOT NULL COMMENT '公司',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '金额',
    sales_ratio   DECIMAL(10, 2) DEFAULT 0 COMMENT '销售占比',
    trend         TINYINT DEFAULT 0 COMMENT '升降 1上升 -1下降 0持平',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售排名';

CREATE TABLE IF NOT EXISTS biz_inventory (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_name  VARCHAR(100) NOT NULL COMMENT '品名',
    spec          VARCHAR(100) COMMENT '规格',
    quantity      DECIMAL(18, 2) DEFAULT 0 COMMENT '数量',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '金额',
    warehouse     VARCHAR(100) COMMENT '分库',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='汾源酒库存';

CREATE TABLE IF NOT EXISTS biz_product_structure (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    category         VARCHAR(100) NOT NULL COMMENT '品类',
    quantity         DECIMAL(18, 2) DEFAULT 0 COMMENT '数量',
    ratio            DECIMAL(10, 2) DEFAULT 0 COMMENT '占比',
    customer_source  VARCHAR(100) COMMENT '客户来源',
    create_by        BIGINT COMMENT '创建人',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by        BIGINT COMMENT '更新人',
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag         TINYINT DEFAULT 0 COMMENT '删除标志',
    remark           VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售产品结构';

CREATE TABLE IF NOT EXISTS biz_customer_dev (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(100) NOT NULL COMMENT '名称',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '金额',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户开发';

CREATE TABLE IF NOT EXISTS fin_receivable (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(100) NOT NULL COMMENT '名称',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '金额',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款明细';

-- -------------------- 初始化数据 --------------------

INSERT INTO sys_dept (id, parent_id, name, code, leader, phone, sort, status, del_flag) VALUES
(1, 0, '汾源酒业集团', 'FYJT', '张总', '0350-1000000', 1, 1, 0),
(2, 1, '总经办', 'ZJB', '李主任', '0350-1000001', 1, 1, 0),
(3, 1, '销售部', 'XSB', '王经理', '0350-1000002', 2, 1, 0),
(4, 1, '生产部', 'SCB', '赵经理', '0350-1000003', 3, 1, 0),
(5, 1, '财务部', 'CWB', '钱经理', '0350-1000004', 4, 1, 0);

-- 密码：admin123（BCrypt）
INSERT INTO sys_user (id, username, password, nickname, real_name, email, phone, dept_id, status, is_admin, del_flag) VALUES
(1, 'admin', '$2a$10$FvqDf2750EP972HV76MnOOb44BqPE9E0Y2NqguqnTdO6heekJaBxO', '超级管理员', '系统管理员', 'admin@fenyuan.com', '13800138000', 1, 1, 1, 0);

INSERT INTO sys_role (id, name, code, description, sort, status, data_scope, del_flag) VALUES
(1, '超级管理员', 'ROLE_ADMIN', '系统最高权限角色', 1, 1, 1, 0),
(2, '普通用户', 'ROLE_USER', '普通用户角色', 2, 1, 3, 0);

INSERT INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(1, 0, '系统管理', '/system', 'Layout', NULL, 0, 'SettingOutlined', 1, 1, 1, 0),
(2, 1, '用户管理', '/system/user', 'system/user/index', 'system:user:list', 1, 'UserOutlined', 1, 1, 1, 0),
(3, 1, '角色管理', '/system/role', 'system/role/index', 'system:role:list', 1, 'TeamOutlined', 2, 1, 1, 0),
(4, 1, '菜单管理', '/system/menu', 'system/menu/index', 'system:menu:list', 1, 'MenuOutlined', 3, 1, 1, 0),
(5, 1, '部门管理', '/system/dept', 'system/dept/index', 'system:dept:list', 1, 'ApartmentOutlined', 4, 1, 1, 0),
(6, 0, '日志管理', '/log', 'Layout', NULL, 0, 'FileTextOutlined', 2, 1, 1, 0),
(7, 6, '操作日志', '/log/oper', 'log/oper/index', 'log:oper:list', 1, 'FileSearchOutlined', 1, 1, 1, 0),
(8, 6, '登录日志', '/log/login', 'log/login/index', 'log:login:list', 1, 'LoginOutlined', 2, 1, 1, 0),
(9, 0, '系统配置', '/settings', 'Layout', NULL, 0, 'ToolOutlined', 3, 1, 1, 0),
(10, 9, '参数配置', '/settings/config', 'settings/config/index', 'system:config:list', 1, 'SettingOutlined', 1, 1, 1, 0),
(11, 2, '新增用户', NULL, NULL, 'system:user:add', 2, NULL, 1, 1, 1, 0),
(12, 2, '编辑用户', NULL, NULL, 'system:user:edit', 2, NULL, 2, 1, 1, 0),
(13, 2, '删除用户', NULL, NULL, 'system:user:delete', 2, NULL, 3, 1, 1, 0),
(14, 2, '重置密码', NULL, NULL, 'system:user:resetPwd', 2, NULL, 4, 1, 1, 0),
(15, 3, '新增角色', NULL, NULL, 'system:role:add', 2, NULL, 1, 1, 1, 0),
(16, 3, '编辑角色', NULL, NULL, 'system:role:edit', 2, NULL, 2, 1, 1, 0),
(17, 3, '删除角色', NULL, NULL, 'system:role:delete', 2, NULL, 3, 1, 1, 0),
(18, 3, '分配权限', NULL, NULL, 'system:role:permission', 2, NULL, 4, 1, 1, 0),
(19, 4, '新增菜单', NULL, NULL, 'system:menu:add', 2, NULL, 1, 1, 1, 0),
(20, 4, '编辑菜单', NULL, NULL, 'system:menu:edit', 2, NULL, 2, 1, 1, 0),
(21, 4, '删除菜单', NULL, NULL, 'system:menu:delete', 2, NULL, 3, 1, 1, 0),
(22, 5, '新增部门', NULL, NULL, 'system:dept:add', 2, NULL, 1, 1, 1, 0),
(23, 5, '编辑部门', NULL, NULL, 'system:dept:edit', 2, NULL, 2, 1, 1, 0),
(24, 5, '删除部门', NULL, NULL, 'system:dept:delete', 2, NULL, 3, 1, 1, 0),
(25, 10, '修改配置', NULL, NULL, 'system:config:edit', 2, NULL, 1, 1, 1, 0),
(26, 0, '业务管理', '/business', 'Layout', NULL, 0, 'ShopOutlined', 4, 1, 1, 0),
(27, 26, '在线销售管理', '/business/online-sale', 'business/onlineSale/index', 'business:onlineSale:list', 1, 'ShoppingCartOutlined', 1, 1, 1, 0),
(28, 26, '销售排名', '/business/sales-rank', 'business/salesRank/index', 'business:salesRank:list', 1, 'TrophyOutlined', 2, 1, 1, 0),
(29, 26, '汾源酒库存', '/business/inventory', 'business/inventory/index', 'business:inventory:list', 1, 'DatabaseOutlined', 3, 1, 1, 0),
(30, 26, '销售产品结构', '/business/product-structure', 'business/productStructure/index', 'business:productStructure:list', 1, 'PieChartOutlined', 4, 1, 1, 0),
(31, 26, '客户开发', '/business/customer-dev', 'business/customerDev/index', 'business:customerDev:list', 1, 'SolutionOutlined', 5, 1, 1, 0),
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

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE del_flag = 0;

INSERT INTO sys_config (id, name, config_key, config_value, type, group_name, status, is_system, del_flag) VALUES
(1, '系统名称', 'system.name', '汾源酒业经营体管理系统', 'string', 'system', 1, 1, 0),
(2, '系统Logo', 'system.logo', '/logo.png', 'string', 'system', 1, 1, 0),
(3, '系统版本', 'system.version', 'v1.0.0', 'string', 'system', 1, 1, 0);

INSERT INTO biz_online_sale (id, sale_amount, ship_amount, payment_amount, period_name, del_flag) VALUES
(1, 1280000.00, 980000.00, 860000.00, '2026年1月', 0),
(2, 1560000.00, 1320000.00, 1100000.00, '2026年2月', 0);

INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag) VALUES
(1, '太原经销商', 520000.00, 28.50, 1, 0),
(2, '大同经销商', 410000.00, 22.40, -1, 0),
(3, '临汾经销商', 380000.00, 20.80, 1, 0);

INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag) VALUES
(1, '汾源原浆', '500ml*6', 1200.00, 360000.00, '总库', 0),
(2, '汾源陈酿', '42度 500ml', 800.00, 240000.00, '一号库', 0),
(3, '汾源礼盒', '两瓶装', 350.00, 175000.00, '二号库', 0);

INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag) VALUES
(1, '原浆系列', 4500.00, 35.00, '经销商', 0),
(2, '陈酿系列', 3200.00, 25.00, '商超', 0),
(3, '礼盒系列', 2100.00, 16.50, '团购', 0);

INSERT INTO biz_customer_dev (id, name, amount, remark, del_flag) VALUES
(1, '晋中商贸有限公司', 86000.00, '新开拓区域经销', 0),
(2, '吕梁烟酒行', 42000.00, '首单合作', 0);

INSERT INTO fin_receivable (id, name, amount, remark, del_flag) VALUES
(1, '太原经销商', 156000.00, '账期30天', 0),
(2, '大同经销商', 98000.00, '部分回款', 0);
