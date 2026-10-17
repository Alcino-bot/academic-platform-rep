CREATE TABLE tipo_unidade (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    nome VARCHAR(100) NOT NULL,
    nivel INTEGER NOT NULL,
    tipo_pai_id UUID,
    modelo_tipo_origem_id UUID REFERENCES modelo_tipo_unidade (id),
    CONSTRAINT uq_tipo_unidade_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_tipo_unidade_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_tipo_unidade_nivel
        CHECK (nivel >= 1),
    CONSTRAINT chk_tipo_unidade_pai
        CHECK ((nivel = 1 AND tipo_pai_id IS NULL) OR (nivel > 1 AND tipo_pai_id IS NOT NULL)),
    CONSTRAINT chk_tipo_unidade_pai_distinto
        CHECK (tipo_pai_id IS DISTINCT FROM id),
    CONSTRAINT fk_tipo_unidade_pai
        FOREIGN KEY (tipo_pai_id, instituicao_id)
        REFERENCES tipo_unidade (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_tipo_unidade_nome
    ON tipo_unidade (instituicao_id, lower(btrim(nome)));

CREATE TABLE unidade_organizacional (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    tipo_unidade_id UUID NOT NULL,
    pai_id UUID,
    nome VARCHAR(200) NOT NULL,
    sigla VARCHAR(30),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_unidade_organizacional_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_unidade_organizacional_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_unidade_organizacional_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT chk_unidade_organizacional_pai_distinto
        CHECK (pai_id IS DISTINCT FROM id),
    CONSTRAINT fk_unidade_organizacional_tipo
        FOREIGN KEY (tipo_unidade_id, instituicao_id)
        REFERENCES tipo_unidade (id, instituicao_id),
    CONSTRAINT fk_unidade_organizacional_pai
        FOREIGN KEY (pai_id, instituicao_id)
        REFERENCES unidade_organizacional (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_unidade_organizacional_nome_irmas
    ON unidade_organizacional (instituicao_id, pai_id, lower(btrim(nome)))
    NULLS NOT DISTINCT;