USE smart_logistics;

CREATE TEMPORARY TABLE test_seed_numbers (
    n INT PRIMARY KEY
) ENGINE=Memory;

INSERT INTO test_seed_numbers(n)
SELECT ones.i + tens.i * 10 + 1 AS n
FROM (
    SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
WHERE ones.i + tens.i * 10 + 1 <= 100;

INSERT INTO waybill(status, total_freight, create_date, priority, clerk, destination_cityid, remark)
SELECT
    CASE n % 4
        WHEN 0 THEN '待发运'
        WHEN 1 THEN '运输中'
        WHEN 2 THEN '已签收'
        ELSE '已取消'
    END AS status,
    0.00 AS total_freight,
    DATE_SUB('2026-06-29 10:00:00', INTERVAL n DAY) AS create_date,
    CASE n % 3
        WHEN 0 THEN '普通'
        WHEN 1 THEN '加急'
        ELSE '特急'
    END AS priority,
    CONCAT('测试员', LPAD((n % 6) + 1, 2, '0')) AS clerk,
    (n % 6) + 1 AS destination_cityid,
    CONCAT('TEST-SEED-20260629-', LPAD(n, 3, '0')) AS remark
FROM test_seed_numbers seed
WHERE NOT EXISTS (
    SELECT 1
    FROM waybill existing
    WHERE existing.remark = CONCAT('TEST-SEED-20260629-', LPAD(seed.n, 3, '0'))
);

INSERT INTO waybill_item(waybillid, goodsid, whid, quantity, base_freight, premium_rate, tax, line_freight)
SELECT
    w.waybillid,
    first_item.goodsid,
    first_item.whid,
    first_item.quantity,
    ROUND(g.declared_price * first_item.quantity * 0.08, 2) AS base_freight,
    CASE w.priority
        WHEN '加急' THEN 0.20
        WHEN '特急' THEN 0.50
        ELSE 0.00
    END AS premium_rate,
    0.06 AS tax,
    ROUND(
        (g.declared_price * first_item.quantity * 0.08)
        + (g.declared_price * first_item.quantity * 0.08
            * CASE w.priority WHEN '加急' THEN 0.20 WHEN '特急' THEN 0.50 ELSE 0.00 END)
        + (g.declared_price * first_item.quantity * 0.06),
        2
    ) AS line_freight
FROM (
    SELECT
        n,
        CASE n % 8
            WHEN 0 THEN 1 WHEN 1 THEN 2 WHEN 2 THEN 3 WHEN 3 THEN 4
            WHEN 4 THEN 5 WHEN 5 THEN 6 WHEN 6 THEN 7 ELSE 8
        END AS goodsid,
        CASE n % 8
            WHEN 0 THEN 1 WHEN 1 THEN 3 WHEN 2 THEN 2 WHEN 3 THEN 4
            WHEN 4 THEN 3 WHEN 5 THEN 6 WHEN 6 THEN 4 ELSE 3
        END AS whid,
        (n % 5) + 1 AS quantity
    FROM test_seed_numbers
) first_item
JOIN waybill w ON w.remark = CONCAT('TEST-SEED-20260629-', LPAD(first_item.n, 3, '0'))
JOIN goods g ON g.goodsid = first_item.goodsid
WHERE NOT EXISTS (
    SELECT
        1
    FROM waybill_item existing
    WHERE existing.waybillid = w.waybillid
      AND existing.goodsid = first_item.goodsid
      AND existing.whid = first_item.whid
);

INSERT INTO waybill_item(waybillid, goodsid, whid, quantity, base_freight, premium_rate, tax, line_freight)
SELECT
    w.waybillid,
    second_item.goodsid,
    second_item.whid,
    second_item.quantity,
    ROUND(g.declared_price * second_item.quantity * 0.08, 2) AS base_freight,
    CASE w.priority
        WHEN '加急' THEN 0.20
        WHEN '特急' THEN 0.50
        ELSE 0.00
    END AS premium_rate,
    0.06 AS tax,
    ROUND(
        (g.declared_price * second_item.quantity * 0.08)
        + (g.declared_price * second_item.quantity * 0.08
            * CASE w.priority WHEN '加急' THEN 0.20 WHEN '特急' THEN 0.50 ELSE 0.00 END)
        + (g.declared_price * second_item.quantity * 0.06),
        2
    ) AS line_freight
FROM (
    SELECT
        n,
        CASE n % 6
            WHEN 0 THEN 1 WHEN 1 THEN 3 WHEN 2 THEN 6
            WHEN 3 THEN 8 WHEN 4 THEN 5 ELSE 2
        END AS goodsid,
        CASE n % 6
            WHEN 0 THEN 5 WHEN 1 THEN 6 WHEN 2 THEN 2
            WHEN 3 THEN 4 WHEN 4 THEN 5 ELSE 1
        END AS whid,
        (n % 3) + 1 AS quantity
    FROM test_seed_numbers
) second_item
JOIN waybill w ON w.remark = CONCAT('TEST-SEED-20260629-', LPAD(second_item.n, 3, '0'))
JOIN goods g ON g.goodsid = second_item.goodsid
WHERE NOT EXISTS (
    SELECT 1
    FROM waybill_item existing
    WHERE existing.waybillid = w.waybillid
      AND existing.goodsid = second_item.goodsid
      AND existing.whid = second_item.whid
);

UPDATE waybill w
JOIN (
    SELECT waybillid, SUM(line_freight) AS total_freight
    FROM waybill_item
    GROUP BY waybillid
) totals ON totals.waybillid = w.waybillid
SET w.total_freight = totals.total_freight
WHERE w.remark LIKE 'TEST-SEED-20260629-%';

DROP TEMPORARY TABLE test_seed_numbers;
