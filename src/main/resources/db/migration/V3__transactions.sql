CREATE TABLE transaction_categories (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(80)  NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    color      VARCHAR(7),
    icon       VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_categories_user ON transaction_categories(user_id);

CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id       BIGINT NOT NULL REFERENCES financial_accounts(id),
    category_id      BIGINT REFERENCES transaction_categories(id) ON DELETE SET NULL,
    type             VARCHAR(20) NOT NULL,
    direction        VARCHAR(10) NOT NULL,
    amount           NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    transaction_date DATE NOT NULL,
    description      VARCHAR(255) NOT NULL,
    source           VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    notes            VARCHAR(500),
    external_id      VARCHAR(120),
    transfer_pair_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_account  ON transactions(account_id);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_transactions_pair     ON transactions(transfer_pair_id);

-- Evita duplicar transações de importação do mesmo arquivo
CREATE UNIQUE INDEX uq_transactions_external
    ON transactions(account_id, external_id)
    WHERE external_id IS NOT NULL;