CREATE TABLE ano_lectivo (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    nome VARCHAR(50) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANEADO',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ano_lectivo_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_ano_lectivo_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_ano_lectivo_datas
        CHECK (data_fim > data_inicio),
    CONSTRAINT chk_ano_lectivo_estado
        CHECK (estado IN ('PLANEADO', 'ACTIVO', 'ENCERRADO'))
);

CREATE UNIQUE INDEX uq_ano_lectivo_nome
    ON ano_lectivo (instituicao_id, lower(btrim(nome)));

CREATE TABLE periodo (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    ano_lectivo_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ordem INTEGER NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_periodo_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_periodo_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_periodo_ordem
        CHECK (ordem >= 1),
    CONSTRAINT chk_periodo_datas
        CHECK (data_fim > data_inicio),
    CONSTRAINT fk_periodo_ano_lectivo
        FOREIGN KEY (ano_lectivo_id, instituicao_id)
        REFERENCES ano_lectivo (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_periodo_ordem
    ON periodo (ano_lectivo_id, ordem);

CREATE UNIQUE INDEX uq_periodo_nome
    ON periodo (ano_lectivo_id, lower(btrim(nome)));
