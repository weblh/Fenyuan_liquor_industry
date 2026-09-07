-- 客户业务规则调整 2026-08-26
-- 已有库执行：mysql -uroot -p Fenyuan_liquor_industry < backend/sql/update_biz_rules_20260826.sql

USE Fenyuan_liquor_industry;

-- 1. 隐藏应收账款菜单（改周例会分析，不再在系统展示）
UPDATE sys_menu SET visible = 0 WHERE id = 33 AND path = '/finance/receivable';

-- 2. 表结构扩展（列已存在时可忽略报错）
ALTER TABLE biz_online_sale ADD COLUMN customer_name VARCHAR(100) NULL COMMENT 'ERP客户名' AFTER id;
ALTER TABLE biz_product_structure ADD COLUMN series VARCHAR(100) NULL COMMENT '系列' AFTER id;
ALTER TABLE biz_product_structure ADD COLUMN product_name VARCHAR(100) NULL COMMENT '产品' AFTER series;
ALTER TABLE biz_customer_dev ADD COLUMN open_month VARCHAR(20) NULL COMMENT '新开月份' AFTER name;
ALTER TABLE biz_customer_dev ADD COLUMN status TINYINT DEFAULT 1 COMMENT '1=开发成功' AFTER open_month;
ALTER TABLE biz_price_compare ADD COLUMN spec VARCHAR(50) NULL COMMENT '规格ml' AFTER product_name;

-- 3. 清理虚假/演示数据
DELETE FROM biz_customer_maintain WHERE customer_name = '运城名酒汇' OR customer_name = '晋中商贸有限公司' OR customer_name = '吕梁烟酒行';
DELETE FROM biz_customer_dev WHERE name = '晋中商贸有限公司' OR name = '吕梁烟酒行';
DELETE FROM biz_sales_rank WHERE company_name = '太原经销商' OR company_name = '大同经销商' OR company_name = '临汾经销商';
DELETE FROM biz_inventory WHERE product_name = '汾源原浆' OR product_name = '汾源陈酿' OR product_name = '汾源礼盒';
DELETE FROM biz_product_structure WHERE category = '原浆系列' OR category = '陈酿系列' OR category = '礼盒系列';
DELETE FROM fin_receivable WHERE name = '太原经销商' OR name = '大同经销商';
DELETE FROM biz_online_sale WHERE customer_name IS NULL OR customer_name = '';
DELETE FROM biz_offsite_sale WHERE product_name = '汾源原浆' OR product_name = '汾源陈酿' OR product_name = '汾源礼盒';
DELETE FROM biz_price_compare WHERE product_name LIKE '汾源%';

-- 4. 客户维护：管家婆同步记录未复购天数置空（避免全部为0天）
UPDATE biz_customer_maintain
SET days_since_purchase = NULL, alert_status = 0, last_purchase_date = NULL
WHERE remark LIKE '[GJP:%' AND (days_since_purchase = 0 OR days_since_purchase IS NULL);
