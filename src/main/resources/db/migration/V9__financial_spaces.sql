CREATE TABLE financial_spaces (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name               VARCHAR(120) NOT NULL,
    slug               VARCHAR(80)  NOT NULL,
    type               VARCHAR(30)  NOT NULL
        CHECK (type IN ('PERSONAL', 'SOFTWARE_PROJECT', 'FREELANCE', 'ONLINE_STORE', 'BUSINESS')),
    description        VARCHAR(500),
    currency           VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    color              VARCHAR(7),
    icon               VARCHAR(40),
    is_default         BOOLEAN      NOT NULL DEFAULT FALSE,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    default_account_id BIGINT       REFERENCES financial_accounts(id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_financial_spaces_user_slug UNIQUE (user_id, slug)
);

CREATE INDEX idx_financial_spaces_user ON financial_spaces(user_id, active);
CREATE UNIQUE INDEX uq_financial_spaces_user_default
    ON financial_spaces(user_id)
    WHERE is_default = TRUE;

-- Provisiona o espaço padrão "Finanças Pessoais" para todos os usuários existentes
INSERT INTO financial_spaces (user_id, name, slug, type, description, currency, color, icon, is_default, active)
SELECT id,
       'Finanças Pessoais',
       'pessoal',
       'PERSONAL',
       'Espaço financeiro pessoal principal',
       currency,
       '#479FFA',
       'wallet',
       TRUE,
       TRUE
FROM users
ON CONFLICT DO NOTHING;

-- Adiciona referência de espaço às entidades financeiras existentes
ALTER TABLE financial_accounts
    ADD COLUMN space_id BIGINT REFERENCES financial_spaces(id) ON DELETE SET NULL;

ALTER TABLE transactions
    ADD COLUMN space_id BIGINT REFERENCES financial_spaces(id) ON DELETE SET NULL;

ALTER TABLE transaction_categories
    ADD COLUMN space_id BIGINT REFERENCES financial_spaces(id) ON DELETE CASCADE;

ALTER TABLE budgets
    ADD COLUMN space_id BIGINT REFERENCES financial_spaces(id) ON DELETE SET NULL;

ALTER TABLE financial_goals
    ADD COLUMN space_id BIGINT REFERENCES financial_spaces(id) ON DELETE SET NULL;

CREATE INDEX idx_accounts_space     ON financial_accounts(space_id);
CREATE INDEX idx_transactions_space ON transactions(user_id, space_id, transaction_date DESC);
CREATE INDEX idx_categories_space   ON transaction_categories(user_id, space_id);
CREATE INDEX idx_budgets_space      ON budgets(user_id, space_id, reference_month);
CREATE INDEX idx_goals_space        ON financial_goals(user_id, space_id);

-- Backfill seguro para o espaço pessoal padrão
UPDATE financial_accounts fa
SET space_id = fs.id
FROM financial_spaces fs
WHERE fs.user_id = fa.user_id AND fs.is_default = TRUE AND fa.space_id IS NULL;

UPDATE transactions t
SET space_id = fs.id
FROM financial_spaces fs
WHERE fs.user_id = t.user_id AND fs.is_default = TRUE AND t.space_id IS NULL;

UPDATE budgets b
SET space_id = fs.id
FROM financial_spaces fs
WHERE fs.user_id = b.user_id AND fs.is_default = TRUE AND b.space_id IS NULL;

UPDATE financial_goals g
SET space_id = fs.id
FROM financial_spaces fs
WHERE fs.user_id = g.user_id AND fs.is_default = TRUE AND g.space_id IS NULL;

-- Permite categorias globais (space_id IS NULL) e categorias homônimas em projetos distintos
ALTER TABLE transaction_categories DROP CONSTRAINT IF EXISTS uq_categories_user_name;
CREATE UNIQUE INDEX uq_categories_user_global_name
    ON transaction_categories(user_id, name)
    WHERE space_id IS NULL;
CREATE UNIQUE INDEX uq_categories_user_space_name
    ON transaction_categories(user_id, space_id, name)
    WHERE space_id IS NOT NULL;

-- Transferências / aportes virtuais entre espaços (sem duplicar transações)
CREATE TABLE space_transfers (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_space_id      BIGINT        NOT NULL REFERENCES financial_spaces(id) ON DELETE RESTRICT,
    destination_space_id BIGINT        NOT NULL REFERENCES financial_spaces(id) ON DELETE RESTRICT,
    amount               NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency             VARCHAR(3)    NOT NULL DEFAULT 'BRL',
    transfer_date        DATE          NOT NULL,
    description          VARCHAR(255)  NOT NULL,
    journal_entry_id     BIGINT        REFERENCES journal_entries(id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_space_transfer_diff CHECK (source_space_id <> destination_space_id)
);

CREATE INDEX idx_space_transfers_user_date ON space_transfers(user_id, transfer_date DESC);
CREATE INDEX idx_space_transfers_src       ON space_transfers(source_space_id);
CREATE INDEX idx_space_transfers_dst       ON space_transfers(destination_space_id);
