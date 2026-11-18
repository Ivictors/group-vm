ALTER TABLE products
    ADD CONSTRAINT chk_products_sku_not_blank CHECK (length(btrim(sku)) > 0);
