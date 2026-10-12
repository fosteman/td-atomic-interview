-- Trades table
CREATE TABLE IF NOT EXISTS trades (
    trade_id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    client_order_id VARCHAR(255),
    pair VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL,
    notional NUMERIC(20, 2) NOT NULL,
    rate NUMERIC(10, 6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    settlement_date VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0,
    UNIQUE(account_id, client_order_id)
);

CREATE INDEX idx_account_id ON trades(account_id);
CREATE INDEX idx_status ON trades(status);

-- Outbox events for reliable publishing to message broker
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id VARCHAR(36) PRIMARY KEY,
    trade_id VARCHAR(36),
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_published ON outbox_events(published);
CREATE INDEX idx_created_at ON outbox_events(created_at);

-- Positions (portfolio by currency)
CREATE TABLE IF NOT EXISTS positions (
    position_id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(20, 2) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(account_id, currency)
);

-- Mock data
INSERT INTO positions (position_id, account_id, currency, balance, updated_at)
VALUES
    ('pos-usd', 'account-123', 'USD', 1000000, NOW()),
    ('pos-eur', 'account-123', 'EUR', 500000, NOW()),
    ('pos-gbp', 'account-123', 'GBP', 250000, NOW()),
    ('pos-jpy', 'account-123', 'JPY', 10000000, NOW())
ON CONFLICT DO NOTHING;
