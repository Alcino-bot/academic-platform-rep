CREATE TABLE permissao (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    codigo VARCHAR(100) NOT NULL,
    descricao TEXT,
    CONSTRAINT chk_permissao_codigo
        CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$')
);

CREATE UNIQUE INDEX uq_permissao_codigo
    ON permissao (codigo);

CREATE TABLE papel (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    codigo VARCHAR(50) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    ambito VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_papel_id_ambito UNIQUE (id, ambito),
    CONSTRAINT chk_papel_codigo
        CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT chk_papel_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_papel_ambito
        CHECK (ambito IN ('GLOBAL', 'INSTITUCIONAL')),
    CONSTRAINT chk_papel_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_papel_codigo
    ON papel (codigo);

CREATE TABLE papel_permissao (
    papel_id UUID NOT NULL REFERENCES papel (id),
    permissao_id UUID NOT NULL REFERENCES permissao (id),
    PRIMARY KEY (papel_id, permissao_id)
);
