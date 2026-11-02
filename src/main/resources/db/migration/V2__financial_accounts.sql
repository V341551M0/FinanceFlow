CREATE TABLE financial_accounts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             VARCHAR(120) NOT NULL,
    institution      VARCHAR(120) NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    initial_balance  NUMERIC(19,2) NOT NULL DEFAULT 0,
    current_balance  NUMERIC(19,2) NOT NULL DEFAULT 0,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_user ON financial_accounts (user_id);
CREATE INDEX idx_accounts_active ON financial_accounts (user_id, active);