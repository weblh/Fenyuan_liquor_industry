-- 补齐业务/财务表（MySQL 已有库执行）
-- 解决：Table 'fenyuan_liquor_industry.biz_online_sale' doesn't exist

USE Fenyuan_liquor_industry;

CREATE TABLE IF NOT EXISTS biz_online_sale (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    sale_amount   DECIMAL(18, 2) DEFAULT 0 COMMENT '销售金额',
    ship_amount   DECIMAL(18, 2) DEFAULT 0 COMMENT '发货金额',
    payment_amount DECIMAL(18, 2) DEFAULT 0 COMMENT '回款金额',
    period_name   VARCHAR(50) COMMENT '期间',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线销售管理';

CREATE TABLE IF NOT EXISTS biz_sales_rank (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    company_name  VARCHAR(100) NOT NULL COMMENT '公司名称',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '销售额',
    sales_ratio   DECIMAL(10, 2) DEFAULT 0 COMMENT '销售占比',
    trend         TINYINT DEFAULT 0 COMMENT '趋势',
    create_by     BIGINT COMMENT '创建人',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT COMMENT '更新人',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag      TINYINT DEFAULT 0 COMMENT '删除标志',
    remark        VARCHAR(255) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售排名';

CREATE TABLE IF NOT EXISTS biz_inventory (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_name  VARCHAR(100) NOT NULL COMMENT '产品名称',
    spec          VARCHAR(100) COMMENT '规格',
    quantity      DECIMAL(18, 2) DEFAULT 0 COMMENT '数量',
    amount        DECIMAL(18, 2) DEFAULT 0 COMMENT '金额',
    warehouse     VARCHAR(100) COMMENT '仓库',
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

-- 样例数据（表空时写入）
INSERT INTO biz_online_sale (id, sale_amount, ship_amount, payment_amount, period_name, del_flag)
SELECT 1, 1280000.00, 980000.00, 860000.00, '2026年1月', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_online_sale WHERE id = 1);
INSERT INTO biz_online_sale (id, sale_amount, ship_amount, payment_amount, period_name, del_flag)
SELECT 2, 1560000.00, 1320000.00, 1100000.00, '2026年2月', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_online_sale WHERE id = 2);

INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag)
SELECT 1, '太原经销商', 520000.00, 28.50, 1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_sales_rank WHERE id = 1);
INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag)
SELECT 2, '大同经销商', 410000.00, 22.40, -1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_sales_rank WHERE id = 2);
INSERT INTO biz_sales_rank (id, company_name, amount, sales_ratio, trend, del_flag)
SELECT 3, '临汾经销商', 380000.00, 20.80, 1, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_sales_rank WHERE id = 3);

INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag)
SELECT 1, '汾源原浆', '500ml*6', 1200.00, 360000.00, '总库', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_inventory WHERE id = 1);
INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag)
SELECT 2, '汾源陈酿', '42度 500ml', 800.00, 240000.00, '一号库', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_inventory WHERE id = 2);
INSERT INTO biz_inventory (id, product_name, spec, quantity, amount, warehouse, del_flag)
SELECT 3, '汾源礼盒', '两瓶装', 350.00, 175000.00, '二号库', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_inventory WHERE id = 3);

INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag)
SELECT 1, '原浆系列', 4500.00, 35.00, '经销商', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_product_structure WHERE id = 1);
INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag)
SELECT 2, '陈酿系列', 3200.00, 25.00, '商超', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_product_structure WHERE id = 2);
INSERT INTO biz_product_structure (id, category, quantity, ratio, customer_source, del_flag)
SELECT 3, '礼盒系列', 2100.00, 16.50, '团购', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_product_structure WHERE id = 3);

INSERT INTO biz_customer_dev (id, name, amount, remark, del_flag)
SELECT 1, '晋中商贸有限公司', 86000.00, '新开拓区域经销', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_customer_dev WHERE id = 1);
INSERT INTO biz_customer_dev (id, name, amount, remark, del_flag)
SELECT 2, '吕梁烟酒行', 42000.00, '首单合作', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM biz_customer_dev WHERE id = 2);

INSERT INTO fin_receivable (id, name, amount, remark, del_flag)
SELECT 1, '太原经销商', 156000.00, '账期30天', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM fin_receivable WHERE id = 1);
INSERT INTO fin_receivable (id, name, amount, remark, del_flag)
SELECT 2, '大同经销商', 98000.00, '部分回款', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM fin_receivable WHERE id = 2);

INSERT INTO biz_customer_maintain (id, customer_name, contact_phone, last_purchase_date, days_since_purchase, alert_status, remark, del_flag)
SELECT 1, '晋中商贸有限公司', '13800001111', '2026-05-10', 108, 1, '超60天未复购，推送主看板', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_customer_maintain WHERE id = 1);
INSERT INTO biz_customer_maintain (id, customer_name, contact_phone, last_purchase_date, days_since_purchase, alert_status, remark, del_flag)
SELECT 2, '吕梁烟酒行', '13900002222', '2026-07-20', 37, 0, '近期有复购', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_customer_maintain WHERE id = 2);
INSERT INTO biz_customer_maintain (id, customer_name, contact_phone, last_purchase_date, days_since_purchase, alert_status, remark, del_flag)
SELECT 3, '运城名酒汇', '13700003333', '2026-04-01', 147, 1, '重点跟进客户', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_customer_maintain WHERE id = 3);

INSERT INTO biz_price_compare (id, product_name, sale_price, jd_price, tmall_price, crawl_time, remark, del_flag)
SELECT 1, '汾源原浆 500ml', 298.00, 318.00, 309.00, '2026-08-25 10:00:00', '销售价低于京东/天猫', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE id = 1);
INSERT INTO biz_price_compare (id, product_name, sale_price, jd_price, tmall_price, crawl_time, remark, del_flag)
SELECT 2, '汾源陈酿 42度', 168.00, 159.00, 162.00, '2026-08-25 10:00:00', '销售价略高于平台', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE id = 2);

INSERT INTO biz_offsite_sale (id, product_name, quantity, province, city, address, source_type, order_no, remark, del_flag)
SELECT 1, '汾源原浆', 120.00, '广东省', '广州市', '天河区体育西路', 'JD', 'JD20260825001', '京东物流异地单', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_offsite_sale WHERE id = 1);
INSERT INTO biz_offsite_sale (id, product_name, quantity, province, city, address, source_type, order_no, remark, del_flag)
SELECT 2, '汾源陈酿', 80.00, '北京市', '北京市', '朝阳区建国路', 'ERP', 'ERP20260825002', 'ERP异地发货', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_offsite_sale WHERE id = 2);
INSERT INTO biz_offsite_sale (id, product_name, quantity, province, city, address, source_type, order_no, remark, del_flag)
SELECT 3, '汾源礼盒', 45.00, '上海市', '上海市', '浦东新区陆家嘴', 'JD', 'JD20260825003', NULL, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_offsite_sale WHERE id = 3);
INSERT INTO biz_offsite_sale (id, product_name, quantity, province, city, address, source_type, order_no, remark, del_flag)
SELECT 4, '汾源原浆', 60.00, '四川省', '成都市', '武侯区人民南路', 'ERP', 'ERP20260825004', NULL, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_offsite_sale WHERE id = 4);

INSERT INTO fin_kingdee_voucher (id, bank_flow_no, counterparty, amount, flow_date, sync_status, voucher_no, sync_time, remark, del_flag)
SELECT 1, 'BK20260825001', '太原经销商', 50000.00, '2026-08-24', 1, 'KD-PZ-20260824-001', '2026-08-24 18:30:00', '已根据银行流水写入金蝶', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fin_kingdee_voucher WHERE id = 1);
INSERT INTO fin_kingdee_voucher (id, bank_flow_no, counterparty, amount, flow_date, sync_status, voucher_no, sync_time, remark, del_flag)
SELECT 2, 'BK20260825002', '大同经销商', 28000.00, '2026-08-25', 0, NULL, NULL, '待同步金蝶凭证', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fin_kingdee_voucher WHERE id = 2);
