CREATE TABLE ledger_accounts (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    financial_account_id BIGINT UNIQUE REFERENCES financial_accounts(id) ON DELETE SET NULL,
    code                 VARCHAR(60)  NOT NULL,
    name                 VARCHAR(120) NOT NULL,
    account_class        VARCHAR(20)  NOT NULL CHECK (account_class IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    currency             VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ledger_accounts_user_code UNIQUE (user_id, code)
);

CREATE INDEX idx_ledger_accounts_user ON ledger_accounts(user_id);
CREATE INDEX idx_ledger_accounts_fin_acc ON ledger_accounts(financial_account_id);

CREATE TABLE journal_entries (
    id              BIGSERIAL PRIMARY KEY,
    entry_uuid      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(120) NOT NULL,
    entry_date      DATE         NOT NULL,
    effective_date  DATE         NOT NULL,
    description     VARCHAR(255) NOT NULL,
    reference_type  VARCHAR(40)  NOT NULL,
    reference_id    BIGINT,
    record_origin   VARCHAR(30)  NOT NULL DEFAULT 'INTERNAL'
        CHECK (record_origin IN ('INTERNAL', 'EXTERNAL_INSTITUTION', 'IMPORTED', 'SIMULATED')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'POSTED'
        CHECK (status IN ('PENDING', 'POSTED', 'REVERSED', 'SIMULATED')),
    reversal_of_id  BIGINT UNIQUE REFERENCES journal_entries(id) ON DELETE RESTRICT,
    metadata_json   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_journal_entries_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_journal_entries_user_date ON journal_entries(user_id, entry_date DESC);
CREATE INDEX idx_journal_entries_ref ON journal_entries(reference_type, reference_id);

CREATE TABLE ledger_postings (
    id                BIGSERIAL PRIMARY KEY,
    journal_entry_id  BIGINT        NOT NULL REFERENCES journal_entries(id) ON DELETE RESTRICT,
    ledger_account_id BIGINT        NOT NULL REFERENCES ledger_accounts(id) ON DELETE RESTRICT,
    side              VARCHAR(10)   NOT NULL CHECK (side IN ('DEBIT', 'CREDIT')),
    amount            NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency          VARCHAR(3)    NOT NULL DEFAULT 'BRL',
    memo              VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_postings_entry ON ledger_postings(journal_entry_id);
CREATE INDEX idx_ledger_postings_account ON ledger_postings(ledger_account_id);

-- Imutabilidade contábil no nível do banco de dados
CREATE OR REPLACE FUNCTION enforce_ledger_immutability()
RETURNS TRIGGER AS $$ BEGIN IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'Registros contábeis do ledger não podem ser excluídos (\%)', TG_TABLE_NAME; END IF;      IF TG_TABLE_NAME = 'ledger_postings' AND TG_OP = 'UPDATE' THEN RAISE EXCEPTION 'Partidas contábeis (ledger_postings) são imutáveis após criação'; END IF;      IF TG_TABLE_NAME = 'journal_entries' AND TG_OP = 'UPDATE' THEN IF OLD.entry_uuid <> NEW.entry_uuid OR OLD.user_id <> NEW.user_id OR OLD.idempotency_key <> NEW.idempotency_key OR OLD.entry_date <> NEW.entry_date OR OLD.description <> NEW.description OR OLD.record_origin <> NEW.record_origin THEN RAISE EXCEPTION 'Campos centrais de journal_entries são imutáveis'; END IF;          IF OLD.status = 'REVERSED' AND NEW.status <> 'REVERSED' THEN RAISE EXCEPTION 'Um lançamento estornado (REVERSED) não pode mudar de estado'; END IF; END IF;      RETURN NEW; END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_immutable_journal_entries
    BEFORE UPDATE OR DELETE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION enforce_ledger_immutability();

CREATE TRIGGER trg_immutable_ledger_postings
    BEFORE UPDATE OR DELETE ON ledger_postings
    FOR EACH ROW EXECUTE FUNCTION enforce_ledger_immutability();
