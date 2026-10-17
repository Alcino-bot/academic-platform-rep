CREATE TABLE utilizador (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    email VARCHAR(255) NOT NULL,
    nome_completo VARCHAR(200) NOT NULL,
    palavra_passe_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    ultimo_acesso TIMESTAMPTZ,
    CONSTRAINT chk_utilizador_email
        CHECK (length(btrim(email)) > 0 AND position('@' IN email) > 1),
    CONSTRAINT chk_utilizador_nome
        CHECK (length(btrim(nome_completo)) > 0),
    CONSTRAINT chk_utilizador_hash
        CHECK (length(palavra_passe_hash) >= 20),
    CONSTRAINT chk_utilizador_estado
        CHECK (estado IN ('PENDENTE', 'ACTIVO', 'SUSPENSO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_utilizador_email
    ON utilizador (lower(btrim(email)));
