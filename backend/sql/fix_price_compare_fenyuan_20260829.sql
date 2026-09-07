-- 2026-08-29 问题反馈：价格对比写入汾源确认销售价；飞天茅台仅网络价
-- mysql -uroot -p Fenyuan_liquor_industry < backend/sql/fix_price_compare_fenyuan_20260829.sql

USE Fenyuan_liquor_industry;

-- 清理演示比价数据
DELETE FROM biz_price_compare WHERE product_name LIKE '汾源%';

-- 按标准品名 upsert 汾源价（元/件）
INSERT INTO biz_price_compare (product_name, spec, sale_price, remark, del_flag)
SELECT '42度青花20', '500ml', 2300.00, '汾源销售价 2300 元/件（客户确认）', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE product_name = '42度青花20' AND IFNULL(del_flag,0)=0);

UPDATE biz_price_compare SET spec='500ml', sale_price=2300.00, remark='汾源销售价 2300 元/件（客户确认）'
WHERE product_name = '42度青花20';

INSERT INTO biz_price_compare (product_name, spec, sale_price, remark, del_flag)
SELECT '53度青花20', '500ml', 2350.00, '汾源销售价 2350 元/件（客户确认）', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE product_name = '53度青花20' AND IFNULL(del_flag,0)=0);

UPDATE biz_price_compare SET spec='500ml', sale_price=2350.00, remark='汾源销售价 2350 元/件（客户确认）'
WHERE product_name = '53度青花20';

INSERT INTO biz_price_compare (product_name, spec, sale_price, remark, del_flag)
SELECT '53度巴拿马20', '475ml', 1820.00, '汾源销售价 1820 元/件（客户确认）', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE product_name = '53度巴拿马20' AND IFNULL(del_flag,0)=0);

UPDATE biz_price_compare SET spec='475ml', sale_price=1820.00, remark='汾源销售价 1820 元/件（客户确认）'
WHERE product_name = '53度巴拿马20';

INSERT INTO biz_price_compare (product_name, spec, sale_price, remark, del_flag)
SELECT '53度老白汾10', '500ml', 810.00, '汾源销售价 810 元/件（客户确认）', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE product_name = '53度老白汾10' AND IFNULL(del_flag,0)=0);

UPDATE biz_price_compare SET spec='500ml', sale_price=810.00, remark='汾源销售价 810 元/件（客户确认）'
WHERE product_name = '53度老白汾10';

INSERT INTO biz_price_compare (product_name, spec, sale_price, remark, del_flag)
SELECT '53度青花30', '500ml', 0, '比价清单（待维护汾源/官方店价格）', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE product_name = '53度青花30' AND IFNULL(del_flag,0)=0);

INSERT INTO biz_price_compare (product_name, spec, sale_price, remark, del_flag)
SELECT '53度飞天茅台', '500ml', 0, '仅摘取京东/天猫官方店网络实时价（不维护汾源销售价）', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_price_compare WHERE product_name = '53度飞天茅台' AND IFNULL(del_flag,0)=0);

UPDATE biz_price_compare SET spec='500ml', sale_price=0, remark='仅摘取京东/天猫官方店网络实时价（不维护汾源销售价）'
WHERE product_name = '53度飞天茅台';
