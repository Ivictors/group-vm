CREATE TABLE product_price_history (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    amount NUMERIC(19, 2) NOT NULL,
    old_amount NUMERIC(19, 2),
    actor TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_price_history_amount CHECK (amount > 0 AND (old_amount IS NULL OR old_amount > 0)),
    CONSTRAINT chk_price_history_actor CHECK (length(btrim(actor)) > 0)
);

CREATE INDEX ix_price_history_product_order
    ON product_price_history (product_id, changed_at, id);

CREATE FUNCTION reject_price_history_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Price history is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_price_history_immutable
    BEFORE UPDATE OR DELETE ON product_price_history
    FOR EACH ROW EXECUTE FUNCTION reject_price_history_change();
