CREATE TABLE instituicao (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    nome VARCHAR(200) NOT NULL,
    sigla VARCHAR(30) NOT NULL,
    tipo_instituicao_id UUID NOT NULL REFERENCES tipo_instituicao (id),
    modelo_origem_id UUID REFERENCES modelo_organizacional (id),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_instituicao_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_instituicao_sigla
        CHECK (length(btrim(sigla)) > 0),
    CONSTRAINT chk_instituicao_estado
        CHECK (estado IN ('PENDENTE', 'ACTIVA', 'SUSPENSA', 'INACTIVA'))
);

CREATE UNIQUE INDEX uq_instituicao_nome
    ON instituicao (lower(btrim(nome)));

CREATE UNIQUE INDEX uq_instituicao_sigla
    ON instituicao (lower(btrim(sigla)));