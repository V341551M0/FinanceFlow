CREATE TABLE credit_cards (
                              id             BIGSERIAL PRIMARY KEY,
                              user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              name           VARCHAR(80)  NOT NULL,
                              brand          VARCHAR(30)  NOT NULL,
                              last_digits    VARCHAR(4),
                              credit_limit   NUMERIC(19,2) NOT NULL CHECK (credit_limit > 0),
                              closing_day    SMALLINT NOT NULL CHECK (closing_day BETWEEN 1 AND 28),
                              due_day        SMALLINT NOT NULL CHECK (due_day BETWEEN 1 AND 28),
                              active         BOOLEAN NOT NULL DEFAULT TRUE,
                              created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cards_user ON credit_cards(user_id);

CREATE TABLE credit_card_invoices (
                                      id              BIGSERIAL PRIMARY KEY,
                                      card_id         BIGINT NOT NULL REFERENCES credit_cards(id) ON DELETE CASCADE,
                                      reference_month DATE NOT NULL,
                                      closing_date    DATE NOT NULL,
                                      due_date        DATE NOT NULL,
                                      total_amount    NUMERIC(19,2) NOT NULL DEFAULT 0,
                                      status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                                      paid_at         TIMESTAMPTZ,
                                      created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      CONSTRAINT uq_invoices_card_month UNIQUE (card_id, reference_month)
);
CREATE INDEX idx_invoices_card ON credit_card_invoices(card_id, reference_month DESC);

CREATE TABLE credit_card_transactions (
                                          id                    BIGSERIAL PRIMARY KEY,
                                          invoice_id            BIGINT NOT NULL REFERENCES credit_card_invoices(id) ON DELETE CASCADE,
                                          category_id           BIGINT REFERENCES transaction_categories(id) ON DELETE SET NULL,
                                          description           VARCHAR(255) NOT NULL,
                                          amount                NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                                          transaction_date      DATE NOT NULL,
                                          installment_number    SMALLINT,
                                          installment_total     SMALLINT,
                                          installment_group_id  BIGINT,
                                          created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_card_tx_invoice ON credit_card_transactions(invoice_id);
CREATE INDEX idx_card_tx_date    ON credit_card_transactions(transaction_date DESC);
CREATE INDEX idx_card_tx_group   ON credit_card_transactions(installment_group_id);