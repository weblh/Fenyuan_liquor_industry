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
