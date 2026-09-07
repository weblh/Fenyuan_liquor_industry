-- H2 / MySQL compatible schema for 汾源酒业经营体

CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(50),
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    sort        INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    TINYINT DEFAULT 0,
    remark      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) NOT NULL,
    password     VARCHAR(100) NOT NULL,
    nickname     VARCHAR(50),
    real_name    VARCHAR(50),
    email        VARCHAR(100),
    phone        VARCHAR(20),
    avatar       VARCHAR(255),
    sex          TINYINT DEFAULT 0,
    dept_id      BIGINT,
    status       TINYINT DEFAULT 1,
    is_admin     TINYINT DEFAULT 0,
    login_ip     VARCHAR(50),
    login_date   TIMESTAMP,
    create_by    BIGINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag     TINYINT DEFAULT 0,
    remark       VARCHAR(255),
    CONSTRAINT uk_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    sort        INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    data_scope  TINYINT DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    TINYINT DEFAULT 0,
    remark      VARCHAR(255),
    CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    name        VARCHAR(50) NOT NULL,
    path        VARCHAR(200),
    component   VARCHAR(200),
    permission  VARCHAR(100),
    type        TINYINT NOT NULL,
    icon        VARCHAR(50),
    sort        INT DEFAULT 0,
    visible     TINYINT DEFAULT 1,
    keep_alive  TINYINT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    TINYINT DEFAULT 0,
    remark      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id   BIGINT NOT NULL,
    role_id   BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id   BIGINT NOT NULL,
    menu_id   BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50),
    user_id     BIGINT,
    module      VARCHAR(50),
    operation   VARCHAR(200),
    method      VARCHAR(200),
    params      CLOB,
    result      CLOB,
    ip          VARCHAR(50),
    location    VARCHAR(100),
    browser     VARCHAR(50),
    os          VARCHAR(50),
    status      TINYINT,
    error_msg   CLOB,
    time        BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50),
    user_id     BIGINT,
    ip          VARCHAR(50),
    location    VARCHAR(100),
    browser     VARCHAR(50),
    os          VARCHAR(50),
    status      TINYINT,
    msg         VARCHAR(255),
    login_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    config_key  VARCHAR(50) NOT NULL,
    config_value CLOB,
    type        VARCHAR(20) DEFAULT 'string',
    group_name  VARCHAR(50),
    status      TINYINT DEFAULT 1,
    is_system   TINYINT DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    TINYINT DEFAULT 0,
    remark      VARCHAR(255),
    CONSTRAINT uk_config_key UNIQUE (config_key)
);

-- 业务管理：在线销售管理
CREATE TABLE IF NOT EXISTS biz_online_sale (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100),
    sale_amount   DECIMAL(18, 2) DEFAULT 0,
    ship_amount   DECIMAL(18, 2) DEFAULT 0,
    payment_amount DECIMAL(18, 2) DEFAULT 0,
    period_name   VARCHAR(50),
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 业务管理：销售排名
CREATE TABLE IF NOT EXISTS biz_sales_rank (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name  VARCHAR(100) NOT NULL,
    amount        DECIMAL(18, 2) DEFAULT 0,
    sales_ratio   DECIMAL(10, 2) DEFAULT 0,
    trend         TINYINT DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 业务管理：汾源酒库存
CREATE TABLE IF NOT EXISTS biz_inventory (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name  VARCHAR(100) NOT NULL,
    spec          VARCHAR(100),
    quantity      DECIMAL(18, 2) DEFAULT 0,
    amount        DECIMAL(18, 2) DEFAULT 0,
    warehouse     VARCHAR(100),
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 业务管理：销售产品结构
CREATE TABLE IF NOT EXISTS biz_product_structure (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    series           VARCHAR(100),
    product_name     VARCHAR(100),
    category         VARCHAR(100) NOT NULL,
    quantity         DECIMAL(18, 2) DEFAULT 0,
    ratio            DECIMAL(10, 2) DEFAULT 0,
    customer_source  VARCHAR(100),
    create_by        BIGINT,
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag         TINYINT DEFAULT 0,
    remark           VARCHAR(255)
);

-- 业务管理：客户开发
CREATE TABLE IF NOT EXISTS biz_customer_dev (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    open_month    VARCHAR(20),
    status        TINYINT DEFAULT 1,
    amount        DECIMAL(18, 2) DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 财务管理：应收账款明细
CREATE TABLE IF NOT EXISTS fin_receivable (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    amount        DECIMAL(18, 2) DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 业务管理：客户维护监管（60天未复购预警）
CREATE TABLE IF NOT EXISTS biz_customer_maintain (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name        VARCHAR(100) NOT NULL,
    contact_phone        VARCHAR(50),
    last_purchase_date   DATE,
    days_since_purchase  INT DEFAULT NULL,
    alert_status         TINYINT DEFAULT 0,
    create_by            BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag             TINYINT DEFAULT 0,
    remark               VARCHAR(255)
);

-- 业务管理：酒类价格对比（京东/天猫）
CREATE TABLE IF NOT EXISTS biz_price_compare (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name  VARCHAR(100) NOT NULL,
    spec          VARCHAR(50),
    sale_price    DECIMAL(18, 2) DEFAULT 0,
    jd_price      DECIMAL(18, 2),
    tmall_price   DECIMAL(18, 2),
    crawl_time    TIMESTAMP,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 业务管理：异地销售统计
CREATE TABLE IF NOT EXISTS biz_offsite_sale (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name  VARCHAR(100) NOT NULL,
    quantity      DECIMAL(18, 2) DEFAULT 0,
    province      VARCHAR(50),
    city          VARCHAR(50),
    address       VARCHAR(255),
    source_type   VARCHAR(20),
    order_no      VARCHAR(100),
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 财务管理：金蝶凭证同步（银行流水自动写凭证）
CREATE TABLE IF NOT EXISTS fin_kingdee_voucher (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_flow_no  VARCHAR(100) NOT NULL,
    counterparty  VARCHAR(100),
    amount        DECIMAL(18, 2) DEFAULT 0,
    flow_date     DATE,
    sync_status   TINYINT DEFAULT 0,
    voucher_no    VARCHAR(100),
    sync_time     TIMESTAMP,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag      TINYINT DEFAULT 0,
    remark        VARCHAR(255)
);

-- 金蝶：登录账号密码
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

-- 金蝶：账套配置
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

-- 金蝶：银行流水渠道
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
    qr_fee_exempt INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    enabled INT DEFAULT 1,
    remark VARCHAR(256),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_kingdee_bank_channel_key UNIQUE (channel_key)
);

-- 金蝶：银行流水凭证明细
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
