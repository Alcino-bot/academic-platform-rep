CREATE TABLE token_utilizador (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    utilizador_id UUID NOT NULL REFERENCES utilizador (id),
    tipo VARCHAR(30) NOT NULL,
    hash_token CHAR(64) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em TIMESTAMPTZ NOT NULL,
    invalidado_em TIMESTAMPTZ,
    CONSTRAINT uq_token_utilizador_hash UNIQUE (hash_token),
    CONSTRAINT chk_token_utilizador_tipo
        CHECK (tipo IN ('VERIFICACAO_EMAIL', 'REDEFINICAO_PALAVRA_PASSE')),
    CONSTRAINT chk_token_utilizador_hash
        CHECK (hash_token ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_token_utilizador_validade
        CHECK (expira_em > criado_em)
);

CREATE INDEX ix_token_utilizador_pessoa
    ON token_utilizador (utilizador_id, tipo, criado_em DESC);

CREATE FUNCTION consumir_token(p_hash TEXT, p_tipo TEXT)
RETURNS UUID
LANGUAGE sql
AS $$
    UPDATE token_utilizador
       SET invalidado_em = now()
     WHERE hash_token = p_hash
       AND tipo = p_tipo
       AND invalidado_em IS NULL
       AND expira_em > now()
    RETURNING utilizador_id
$$;
