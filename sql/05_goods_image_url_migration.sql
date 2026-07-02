SET @goods_image_url_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'goods'
      AND COLUMN_NAME = 'image_url'
);

SET @goods_image_url_sql := IF(
    @goods_image_url_exists = 0,
    'ALTER TABLE goods ADD COLUMN image_url VARCHAR(500) NULL AFTER remark',
    'SELECT 1'
);

PREPARE goods_image_url_stmt FROM @goods_image_url_sql;
EXECUTE goods_image_url_stmt;
DEALLOCATE PREPARE goods_image_url_stmt;
