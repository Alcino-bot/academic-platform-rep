CREATE TABLE tipo_instituicao (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_aprovacao TIMESTAMPTZ,
    CONSTRAINT chk_tipo_instituicao_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_tipo_instituicao_estado
        CHECK (estado IN ('PENDENTE', 'APROVADA', 'REJEITADA', 'INACTIVA')),
    CONSTRAINT chk_tipo_instituicao_aprovacao
        CHECK (
            (estado IN ('PENDENTE', 'REJEITADA') AND data_aprovacao IS NULL)
            OR (estado = 'APROVADA' AND data_aprovacao IS NOT NULL)
            OR estado = 'INACTIVA'
        )
);

CREATE UNIQUE INDEX uq_tipo_instituicao_nome
    ON tipo_instituicao (lower(btrim(nome)));