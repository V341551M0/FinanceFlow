CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    email           VARCHAR(180) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    locale          VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    timezone        VARCHAR(50)  NOT NULL DEFAULT 'America/Sao_Paulo',
    theme           VARCHAR(10)  NOT NULL DEFAULT 'DARK',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);