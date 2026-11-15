CREATE TABLE quote_allocations (
    child_id UUID PRIMARY KEY REFERENCES quote_requests(id),
    parent_id UUID NOT NULL REFERENCES quote_requests(id),
    CHECK (child_id <> parent_id)
);
CREATE INDEX idx_quote_allocations_parent ON quote_allocations(parent_id);
