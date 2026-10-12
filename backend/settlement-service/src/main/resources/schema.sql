-- Settlement table
CREATE TABLE IF NOT EXISTS settlements (
    settlement_id VARCHAR(36) PRIMARY KEY,
    trade_id VARCHAR(36) NOT NULL UNIQUE,
    account_id VARCHAR(100) NOT NULL,
    pair VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL,
    notional NUMERIC(20, 2) NOT NULL,
    settlement_date VARCHAR(10) NOT NULL,
    settlement_amount NUMERIC(20, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    nostro_account VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    settled_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trade_id ON settlements(trade_id);
CREATE INDEX IF NOT EXISTS idx_status ON settlements(status);
