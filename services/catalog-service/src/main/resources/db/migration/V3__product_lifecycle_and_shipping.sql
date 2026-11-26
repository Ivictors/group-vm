DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM products
        WHERE position(chr(28) IN sku) > 0 OR position(chr(29) IN sku) > 0
           OR position(chr(30) IN sku) > 0 OR position(chr(31) IN sku) > 0
    ) THEN
        RAISE EXCEPTION 'Legacy SKU contains unsupported control whitespace; correct it before V3';
    END IF;
END $$;

ALTER TABLE products
    ADD COLUMN status VARCHAR(16),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN weight_grams INTEGER,
    ADD COLUMN length_cm NUMERIC,
    ADD COLUMN width_cm NUMERIC,
    ADD COLUMN height_cm NUMERIC;

UPDATE products
SET status = CASE WHEN archived THEN 'ARCHIVED' ELSE 'DRAFT' END,
    sku = regexp_replace(sku, '^[[:space:]]+|[[:space:]]+$', '', 'g');

ALTER TABLE products
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN price TYPE NUMERIC USING price::NUMERIC,
    ADD CONSTRAINT chk_products_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    ADD CONSTRAINT chk_products_version CHECK (version >= 0),
    ADD CONSTRAINT chk_products_weight CHECK (weight_grams IS NULL OR weight_grams > 0),
    ADD CONSTRAINT chk_products_length CHECK (length_cm IS NULL OR length_cm > 0),
    ADD CONSTRAINT chk_products_width CHECK (width_cm IS NULL OR width_cm > 0),
    ADD CONSTRAINT chk_products_height CHECK (height_cm IS NULL OR height_cm > 0),
    ADD CONSTRAINT chk_products_active_shipping CHECK (
        status <> 'ACTIVE' OR
        (weight_grams IS NOT NULL AND length_cm IS NOT NULL
         AND width_cm IS NOT NULL AND height_cm IS NOT NULL)),
    ADD CONSTRAINT chk_products_price_scale CHECK (
        price > 0 AND scale(price) = 2 AND price < 100000000000000000),
    ADD CONSTRAINT chk_products_sku_trimmed CHECK (
        sku = regexp_replace(sku, '^[[:space:]]+|[[:space:]]+$', '', 'g')),
    ADD CONSTRAINT chk_products_sku_supported_whitespace CHECK (
        position(chr(28) IN sku) = 0 AND position(chr(29) IN sku) = 0
        AND position(chr(30) IN sku) = 0 AND position(chr(31) IN sku) = 0);

CREATE UNIQUE INDEX uk_products_sku_ci
    ON products (lower(regexp_replace(sku, '^[[:space:]]+|[[:space:]]+$', '', 'g')));

ALTER TABLE products DROP CONSTRAINT uk_products_sku;
ALTER TABLE products DROP COLUMN archived;
