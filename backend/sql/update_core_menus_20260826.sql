-- 经营体核心方向菜单与表结构增量（已有库执行）
-- 2026-08-26

CREATE TABLE IF NOT EXISTS biz_customer_maintain (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    customer_name        VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact_phone        VARCHAR(50) COMMENT '联系电话',
    last_purchase_date   DATE COMMENT '最近购买日',
    days_since_purchase  INT DEFAULT 0 COMMENT '未复购天数',
    alert_status         TINYINT DEFAULT 0 COMMENT '预警状态(0正常1-60天未复购)',
    create_by            BIGINT COMMENT '创建人',
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by            BIGINT COMMENT '更新人',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag             TINYINT DEFAULT 0 COMMENT '删除标志',
    remark               VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户维护监管';

CREATE TABLE IF NOT EXISTS biz_price_compare (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_name  VARCHAR(100) NOT NULL COMMENT '产品名称',
    sale_price    DECIMAL(18, 2) DEFAULT 0 COMMENT '目前销售价',
    jd_price      DECIMAL(18, 2) COMMENT '京东价',
    tmall_price   DECIMAL(18, 2) COMMENT '天猫价',
    crawl_time    DATETIME COMMENT '抓取时间',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒类价格对比';

CREATE TABLE IF NOT EXISTS biz_offsite_sale (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_name  VARCHAR(100) NOT NULL COMMENT '产品名称',
    quantity      DECIMAL(18, 2) DEFAULT 0 COMMENT '销量',
    province      VARCHAR(50) COMMENT '省份',
    city          VARCHAR(50) COMMENT '城市',
    address       VARCHAR(255) COMMENT '详细地址',
    source_type   VARCHAR(20) COMMENT '来源ERP/JD',
    order_no      VARCHAR(100) COMMENT '单据号',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异地销售统计';

CREATE TABLE IF NOT EXISTS fin_kingdee_voucher (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    bank_flow_no  VARCHAR(100) NOT NULL COMMENT '银行流水号',
    counterparty  VARCHAR(100) COMMENT '对方户名',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '金额',
    flow_date     DATE COMMENT '流水日期',
    sync_status   TINYINT DEFAULT 0 COMMENT '同步状态(0待同步1已写入2失败)',
    voucher_no    VARCHAR(100) COMMENT '金蝶凭证号',
    sync_time     DATETIME COMMENT '同步时间',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金蝶凭证同步';

-- 调整原业务菜单排序
UPDATE sys_menu SET sort = 4 WHERE id = 27;
UPDATE sys_menu SET sort = 5 WHERE id = 28;
UPDATE sys_menu SET sort = 6 WHERE id = 29;
UPDATE sys_menu SET sort = 7 WHERE id = 30;
UPDATE sys_menu SET sort = 8 WHERE id = 31;

-- 新菜单（忽略已存在）
INSERT IGNORE INTO sys_menu (id, parent_id, name, path, component, permission, type, icon, sort, visible, status, del_flag) VALUES
(52, 26, '客户维护监管', '/business/customer-maintain', 'business/customerMaintain/index', 'business:customerMaintain:list', 1, 'CustomerServiceOutlined', 1, 1, 1, 0),
(53, 52, '新增', NULL, NULL, 'business:customerMaintain:add', 2, NULL, 1, 1, 1, 0),
(54, 52, '编辑', NULL, NULL, 'business:customerMaintain:edit', 2, NULL, 2, 1, 1, 0),
(55, 52, '删除', NULL, NULL, 'business:customerMaintain:delete', 2, NULL, 3, 1, 1, 0),
(56, 26, '酒类价格对比', '/business/price-compare', 'business/priceCompare/index', 'business:priceCompare:list', 1, 'FundOutlined', 2, 1, 1, 0),
(57, 56, '新增', NULL, NULL, 'business:priceCompare:add', 2, NULL, 1, 1, 1, 0),
(58, 56, '编辑', NULL, NULL, 'business:priceCompare:edit', 2, NULL, 2, 1, 1, 0),
(59, 56, '删除', NULL, NULL, 'business:priceCompare:delete', 2, NULL, 3, 1, 1, 0),
(60, 26, '异地销售统计', '/business/offsite-sales', 'business/offsiteSales/index', 'business:offsiteSale:list', 1, 'EnvironmentOutlined', 3, 1, 1, 0),
(61, 60, '新增', NULL, NULL, 'business:offsiteSale:add', 2, NULL, 1, 1, 1, 0),
(62, 60, '编辑', NULL, NULL, 'business:offsiteSale:edit', 2, NULL, 2, 1, 1, 0),
(63, 60, '删除', NULL, NULL, 'business:offsiteSale:delete', 2, NULL, 3, 1, 1, 0),
(64, 32, '金蝶凭证同步', '/finance/kingdee-voucher', 'finance/kingdeeVoucher/index', 'finance:kingdeeVoucher:list', 1, 'FileSyncOutlined', 2, 1, 1, 0),
(65, 64, '新增', NULL, NULL, 'finance:kingdeeVoucher:add', 2, NULL, 1, 1, 1, 0),
(66, 64, '编辑', NULL, NULL, 'finance:kingdeeVoucher:edit', 2, NULL, 2, 1, 1, 0),
(67, 64, '删除', NULL, NULL, 'finance:kingdeeVoucher:delete', 2, NULL, 3, 1, 1, 0);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 52 AND 67;
