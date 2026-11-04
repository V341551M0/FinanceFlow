CREATE TABLE budgets (
                         id             BIGSERIAL PRIMARY KEY,
                         user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         category_id    BIGINT REFERENCES transaction_categories(id) ON DELETE CASCADE,
                         reference_month DATE NOT NULL,
                         limit_amount   NUMERIC(19,2) NOT NULL CHECK (limit_amount > 0),
                         created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         CONSTRAINT uq_budgets_user_cat_month UNIQUE (user_id, category_id, reference_month)
);
CREATE INDEX idx_budgets_user_month ON budgets(user_id, reference_month);

CREATE TABLE financial_goals (
                                 id             BIGSERIAL PRIMARY KEY,
                                 user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 name           VARCHAR(120) NOT NULL,
                                 target_amount  NUMERIC(19,2) NOT NULL CHECK (target_amount > 0),
                                 current_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
                                 target_date    DATE,
                                 completed      BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_goals_user ON financial_goals(user_id, completed);