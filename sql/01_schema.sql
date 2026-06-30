DROP DATABASE IF EXISTS smart_logistics;
CREATE DATABASE smart_logistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE smart_logistics;

CREATE TABLE hub_region (
    regionid BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL UNIQUE,
    remark VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE city (
    cityid BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    regionid BIGINT NOT NULL,
    remark VARCHAR(255),
    CONSTRAINT fk_city_region FOREIGN KEY (regionid) REFERENCES hub_region(regionid),
    UNIQUE KEY uk_city_region (name, regionid),
    KEY idx_city_region (regionid)
) ENGINE=InnoDB;

CREATE TABLE goods (
    goodsid BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    brand VARCHAR(80),
    declared_price DECIMAL(12,2) NOT NULL,
    remark VARCHAR(255),
    KEY idx_goods_category (category),
    KEY idx_goods_value (declared_price)
) ENGINE=InnoDB;

CREATE TABLE supplier (
    suppid BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    address VARCHAR(255),
    cityid BIGINT NOT NULL,
    phone VARCHAR(30),
    remark VARCHAR(255),
    CONSTRAINT fk_supplier_city FOREIGN KEY (cityid) REFERENCES city(cityid),
    KEY idx_supplier_city (cityid)
) ENGINE=InnoDB;

CREATE TABLE warehouse (
    whid BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    capacity DECIMAL(14,2) NOT NULL,
    cityid BIGINT NOT NULL,
    contact VARCHAR(80),
    remark VARCHAR(255),
    CONSTRAINT fk_warehouse_city FOREIGN KEY (cityid) REFERENCES city(cityid),
    KEY idx_warehouse_city (cityid)
) ENGINE=InnoDB;

CREATE TABLE inventory (
    whid BIGINT NOT NULL,
    goodsid BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    frozen_quantity INT NOT NULL DEFAULT 0,
    safety_stock INT NOT NULL DEFAULT 10,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (whid, goodsid),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (whid) REFERENCES warehouse(whid),
    CONSTRAINT fk_inventory_goods FOREIGN KEY (goodsid) REFERENCES goods(goodsid),
    CONSTRAINT ck_inventory_non_negative CHECK (quantity >= 0 AND frozen_quantity >= 0),
    KEY idx_inventory_goods_quantity (goodsid, quantity)
) ENGINE=InnoDB;

CREATE TABLE waybill (
    waybillid BIGINT PRIMARY KEY AUTO_INCREMENT,
    status ENUM('待发运','运输中','已签收','已取消') NOT NULL DEFAULT '待发运',
    total_freight DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    priority ENUM('普通','加急','特急') NOT NULL DEFAULT '普通',
    clerk VARCHAR(80) NOT NULL,
    destination_cityid BIGINT NOT NULL,
    remark VARCHAR(255),
    CONSTRAINT fk_waybill_destination FOREIGN KEY (destination_cityid) REFERENCES city(cityid),
    KEY idx_waybill_create_date (create_date),
    KEY idx_waybill_status (status),
    KEY idx_waybill_destination (destination_cityid)
) ENGINE=InnoDB;

CREATE TABLE waybill_item (
    itemid BIGINT PRIMARY KEY AUTO_INCREMENT,
    waybillid BIGINT NOT NULL,
    goodsid BIGINT NOT NULL,
    whid BIGINT NOT NULL,
    quantity INT NOT NULL,
    base_freight DECIMAL(12,2) NOT NULL,
    premium_rate DECIMAL(5,2) NOT NULL,
    tax DECIMAL(5,2) NOT NULL,
    line_freight DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_item_waybill FOREIGN KEY (waybillid) REFERENCES waybill(waybillid) ON DELETE CASCADE,
    CONSTRAINT fk_item_goods FOREIGN KEY (goodsid) REFERENCES goods(goodsid),
    CONSTRAINT fk_item_warehouse FOREIGN KEY (whid) REFERENCES warehouse(whid),
    KEY idx_item_waybill (waybillid),
    KEY idx_item_goods (goodsid),
    KEY idx_item_warehouse (whid)
) ENGINE=InnoDB;

CREATE TABLE stock_txn (
    txnid BIGINT PRIMARY KEY AUTO_INCREMENT,
    whid BIGINT NOT NULL,
    goodsid BIGINT NOT NULL,
    waybillid BIGINT,
    change_quantity INT NOT NULL,
    txn_type ENUM('入库','出库冻结','取消释放','签收出库') NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_name VARCHAR(80),
    remark VARCHAR(255),
    CONSTRAINT fk_txn_warehouse FOREIGN KEY (whid) REFERENCES warehouse(whid),
    CONSTRAINT fk_txn_goods FOREIGN KEY (goodsid) REFERENCES goods(goodsid),
    CONSTRAINT fk_txn_waybill FOREIGN KEY (waybillid) REFERENCES waybill(waybillid),
    KEY idx_txn_goods_time (goodsid, create_time),
    KEY idx_txn_wh_goods (whid, goodsid)
) ENGINE=InnoDB;

CREATE TABLE region_distance (
    from_regionid BIGINT NOT NULL,
    to_regionid BIGINT NOT NULL,
    distance_level INT NOT NULL,
    PRIMARY KEY (from_regionid, to_regionid),
    CONSTRAINT fk_distance_from FOREIGN KEY (from_regionid) REFERENCES hub_region(regionid),
    CONSTRAINT fk_distance_to FOREIGN KEY (to_regionid) REFERENCES hub_region(regionid)
) ENGINE=InnoDB;
