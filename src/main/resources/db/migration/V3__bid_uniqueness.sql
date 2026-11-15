-- One bid per workshop per quotation, also enforced under concurrent requests.
CREATE UNIQUE INDEX uq_bid_quote_artisan ON bids (quote_request_id, artisan_id);
