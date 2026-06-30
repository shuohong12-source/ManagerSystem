USE smart_logistics;

CREATE OR REPLACE VIEW v_region_freight_volume AS
SELECT
    r.regionid,
    r.name AS region_name,
    COALESCE(SUM(i.quantity), 0) AS total_quantity,
    COALESCE(SUM(i.line_freight), 0) AS total_freight
FROM hub_region r
LEFT JOIN city c ON c.regionid = r.regionid
LEFT JOIN waybill w ON w.destination_cityid = c.cityid
LEFT JOIN waybill_item i ON i.waybillid = w.waybillid
GROUP BY r.regionid, r.name;

CREATE OR REPLACE VIEW v_top_goods_value AS
SELECT
    g.goodsid,
    g.name AS goods_name,
    g.category,
    g.brand,
    SUM(i.quantity) AS ship_quantity,
    SUM(i.quantity * g.declared_price) AS declared_value,
    SUM(i.line_freight) AS freight_amount
FROM goods g
JOIN waybill_item i ON i.goodsid = g.goodsid
GROUP BY g.goodsid, g.name, g.category, g.brand
ORDER BY declared_value DESC;

DELIMITER //

CREATE PROCEDURE sp_monthly_freight_report(IN p_month CHAR(7))
BEGIN
    SELECT
        DATE_FORMAT(w.create_date, '%Y-%m') AS month_value,
        r.name AS destination_region,
        c.name AS destination_city,
        w.status,
        COUNT(DISTINCT w.waybillid) AS waybill_count,
        COALESCE(SUM(i.quantity), 0) AS goods_quantity,
        COALESCE(SUM(w.total_freight), 0) AS freight_amount
    FROM waybill w
    JOIN city c ON c.cityid = w.destination_cityid
    JOIN hub_region r ON r.regionid = c.regionid
    LEFT JOIN waybill_item i ON i.waybillid = w.waybillid
    WHERE DATE_FORMAT(w.create_date, '%Y-%m') = p_month
    GROUP BY DATE_FORMAT(w.create_date, '%Y-%m'), r.name, c.name, w.status
    ORDER BY r.name, c.name, w.status;
END//

CREATE PROCEDURE sp_recommend_warehouse(IN p_destination_cityid BIGINT, IN p_goodsid BIGINT)
BEGIN
    SELECT
        wh.whid,
        wh.name AS warehouse_name,
        wc.name AS warehouse_city,
        wr.name AS warehouse_region,
        inv.quantity,
        CASE WHEN wh.cityid = p_destination_cityid THEN 0 ELSE 1 END AS city_rank,
        rd.distance_level
    FROM warehouse wh
    JOIN city wc ON wc.cityid = wh.cityid
    JOIN hub_region wr ON wr.regionid = wc.regionid
    JOIN inventory inv ON inv.whid = wh.whid AND inv.goodsid = p_goodsid
    JOIN city dc ON dc.cityid = p_destination_cityid
    JOIN region_distance rd ON rd.from_regionid = wc.regionid AND rd.to_regionid = dc.regionid
    WHERE inv.quantity > 0
    ORDER BY city_rank, rd.distance_level, inv.quantity DESC;
END//

DELIMITER ;
