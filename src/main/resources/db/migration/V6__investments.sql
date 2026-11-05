CREATE TABLE assets (
                        id          BIGSERIAL PRIMARY KEY,
                        symbol      VARCHAR(20)  NOT NULL,
                        name        VARCHAR(120) NOT NULL,
                        type        VARCHAR(20)  NOT NULL,
                        exchange    VARCHAR(20),
                        currency    VARCHAR(3)   NOT NULL DEFAULT 'BRL',
                        created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uq_assets_symbol_exchange UNIQUE (symbol, exchange)
);
CREATE INDEX idx_assets_symbol ON assets(symbol);

CREATE TABLE investments (
                             id             BIGSERIAL PRIMARY KEY,
                             user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             asset_id       BIGINT NOT NULL REFERENCES assets(id),
                             quantity       NUMERIC(19,8) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                             average_price  NUMERIC(19,8) NOT NULL DEFAULT 0 CHECK (average_price >= 0),
                             created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             CONSTRAINT uq_investments_user_asset UNIQUE (user_id, asset_id)
);
CREATE INDEX idx_investments_user ON investments(user_id);

CREATE TABLE investment_operations (
                                       id              BIGSERIAL PRIMARY KEY,
                                       investment_id   BIGINT NOT NULL REFERENCES investments(id) ON DELETE CASCADE,
                                       type            VARCHAR(10) NOT NULL,
                                       quantity        NUMERIC(19,8) NOT NULL CHECK (quantity > 0),
                                       price           NUMERIC(19,8) NOT NULL CHECK (price >= 0),
                                       operation_date  DATE NOT NULL,
                                       fees            NUMERIC(19,2) NOT NULL DEFAULT 0,
                                       notes           VARCHAR(255),
                                       created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_operations_investment ON investment_operations(investment_id, operation_date DESC);

CREATE TABLE watchlist_items (
                                 id          BIGSERIAL PRIMARY KEY,
                                 user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 asset_id    BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
                                 created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 CONSTRAINT uq_watchlist_user_asset UNIQUE (user_id, asset_id)
);
CREATE INDEX idx_watchlist_user ON watchlist_items(user_id);