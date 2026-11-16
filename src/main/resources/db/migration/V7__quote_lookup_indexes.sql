CREATE INDEX idx_quotes_buyer_status_created ON quote_requests(buyer_id, status, created_at DESC);
CREATE INDEX idx_quotes_targets_gin ON quote_requests USING GIN(target_artisan_ids jsonb_path_ops);
CREATE INDEX idx_products_batch ON products(batch_id);
