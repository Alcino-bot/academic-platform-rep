CREATE TABLE modelo_organizacional (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    tipo_instituicao_id UUID NOT NULL REFERENCES tipo_instituicao (id),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_modelo_organizacional_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_modelo_organizacional_estado
        CHECK (estado IN ('RASCUNHO', 'ACTIVO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_modelo_organizacional_nome
    ON modelo_organizacional (tipo_instituicao_id, lower(btrim(nome)));

CREATE TABLE modelo_tipo_unidade (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    modelo_id UUID NOT NULL REFERENCES modelo_organizacional (id),
    nome VARCHAR(100) NOT NULL,
    nivel INTEGER NOT NULL,
    tipo_pai_id UUID,
    CONSTRAINT uq_modelo_tipo_unidade_id_modelo UNIQUE (id, modelo_id),
    CONSTRAINT chk_modelo_tipo_unidade_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_modelo_tipo_unidade_nivel
        CHECK (nivel >= 1),
    CONSTRAINT chk_modelo_tipo_unidade_pai
        CHECK ((nivel = 1 AND tipo_pai_id IS NULL) OR (nivel > 1 AND tipo_pai_id IS NOT NULL)),
    CONSTRAINT chk_modelo_tipo_unidade_pai_distinto
        CHECK (tipo_pai_id IS DISTINCT FROM id),
    CONSTRAINT fk_modelo_tipo_unidade_pai
        FOREIGN KEY (tipo_pai_id, modelo_id)
        REFERENCES modelo_tipo_unidade (id, modelo_id)
);

CREATE UNIQUE INDEX uq_modelo_tipo_unidade_nome
    ON modelo_tipo_unidade (modelo_id, lower(btrim(nome)));