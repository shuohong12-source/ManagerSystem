USE smart_logistics;

INSERT INTO hub_region(regionid, name, remark) VALUES
(1, '华东枢纽', '上海、杭州、宁波跨境集散区'),
(2, '华南枢纽', '广州、深圳、香港方向'),
(3, '华北枢纽', '北京、天津方向'),
(4, '西南枢纽', '成都、重庆方向');

INSERT INTO city(cityid, name, regionid, remark) VALUES
(1, '上海', 1, '海运与空运核心口岸'),
(2, '杭州', 1, '电商集货城市'),
(3, '广州', 2, '华南外贸中心'),
(4, '深圳', 2, '电子产品出口中心'),
(5, '北京', 3, '北方枢纽城市'),
(6, '成都', 4, '西南航空物流中心');

INSERT INTO goods(goodsid, name, category, brand, declared_price, remark, image_url) VALUES
(1, '智能手表 X2', '电子产品', 'StarLink', 799.00, '高价值小件', '/goods/electronics.svg'),
(2, '蓝牙耳机 Pro', '电子产品', 'SoundFox', 399.00, '易损需防震', '/goods/electronics.svg'),
(3, '真丝围巾', '服饰', 'SilkWay', 168.00, '轻泡货', '/goods/apparel.svg'),
(4, '咖啡豆 1kg', '食品', 'Andes', 96.00, '注意保质期', '/goods/food.svg'),
(5, '机械键盘 K87', '电子产品', 'KeyLab', 529.00, '标准箱规', '/goods/electronics.svg'),
(6, '运动鞋 Aero', '服饰', 'RunMax', 459.00, '热门商品', '/goods/apparel.svg'),
(7, '护肤套装', '美妆', 'Lumi', 328.00, '避免高温', '/goods/beauty.svg'),
(8, '车载充电器', '汽车用品', 'VoltGo', 129.00, '小件配件', '/goods/auto.svg');

INSERT INTO supplier(suppid, name, address, cityid, phone, remark) VALUES
(1, '上海星链科技有限公司', '上海市浦东新区创新路88号', 1, '021-88008800', '电子产品供应商'),
(2, '深圳速联电子有限公司', '深圳市南山区科技园9号', 4, '0755-66886688', '电子配件供应商'),
(3, '杭州丝路贸易有限公司', '杭州市余杭区仓前街道18号', 2, '0571-88990011', '服饰供应商'),
(4, '广州优美日化有限公司', '广州市白云区云城路66号', 3, '020-77889900', '美妆供应商');

INSERT INTO warehouse(whid, name, capacity, cityid, contact, remark) VALUES
(1, '上海浦东保税仓', 20000.00, 1, '王明', '近机场'),
(2, '杭州电商云仓', 16000.00, 2, '李娜', '适合轻小件'),
(3, '深圳前海仓', 18000.00, 4, '陈杰', '电子产品优先'),
(4, '广州南沙仓', 22000.00, 3, '赵琪', '海运便利'),
(5, '北京顺义仓', 12000.00, 5, '周航', '北方中转'),
(6, '成都双流仓', 10000.00, 6, '林川', '西南中转');

INSERT INTO inventory(whid, goodsid, quantity, frozen_quantity, safety_stock) VALUES
(1, 1, 120, 0, 20), (1, 2, 18, 12, 30), (1, 3, 90, 0, 20),
(2, 3, 260, 0, 30), (2, 4, 12, 8, 20), (2, 6, 110, 0, 20),
(3, 1, 220, 0, 30), (3, 2, 310, 0, 30), (3, 5, 160, 0, 20), (3, 8, 300, 0, 30),
(4, 4, 260, 0, 30), (4, 7, 19, 6, 20), (4, 8, 140, 0, 20),
(5, 1, 15, 5, 15), (5, 5, 80, 0, 15), (5, 6, 70, 0, 15),
(6, 3, 100, 0, 15), (6, 6, 120, 0, 20), (6, 7, 90, 0, 15);

INSERT INTO region_distance(from_regionid, to_regionid, distance_level)
SELECT a.regionid, b.regionid,
       CASE
           WHEN a.regionid = b.regionid THEN 0
           WHEN ABS(a.regionid - b.regionid) = 1 THEN 1
           ELSE 2
       END
FROM hub_region a
CROSS JOIN hub_region b;
