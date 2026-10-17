CREATE TABLE curso (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    unidade_id UUID NOT NULL,
    codigo VARCHAR(30),
    nome VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_curso_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_curso_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_curso_codigo
        CHECK (codigo IS NULL OR length(btrim(codigo)) > 0),
    CONSTRAINT chk_curso_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT fk_curso_unidade
        FOREIGN KEY (unidade_id, instituicao_id)
        REFERENCES unidade_organizacional (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_curso_nome
    ON curso (instituicao_id, unidade_id, lower(btrim(nome)));

CREATE UNIQUE INDEX uq_curso_codigo
    ON curso (instituicao_id, lower(btrim(codigo)))
    WHERE codigo IS NOT NULL;

CREATE TABLE disciplina (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    unidade_id UUID,
    codigo VARCHAR(30) NOT NULL,
    nome VARCHAR(200) NOT NULL,
    carga_horaria INTEGER,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_disciplina_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_disciplina_codigo
        CHECK (length(btrim(codigo)) > 0),
    CONSTRAINT chk_disciplina_nome
        CHECK (length(btrim(nome)) > 0),
    CONSTRAINT chk_disciplina_carga_horaria
        CHECK (carga_horaria IS NULL OR carga_horaria > 0),
    CONSTRAINT chk_disciplina_estado
        CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT fk_disciplina_unidade
        FOREIGN KEY (unidade_id, instituicao_id)
        REFERENCES unidade_organizacional (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_disciplina_codigo
    ON disciplina (instituicao_id, lower(btrim(codigo)));

CREATE TABLE curso_disciplina (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    curso_id UUID NOT NULL,
    disciplina_id UUID NOT NULL,
    ano_curricular INTEGER,
    periodo_curricular INTEGER,
    obrigatoria BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_curso_disciplina UNIQUE (curso_id, disciplina_id),
    CONSTRAINT chk_curso_disciplina_ano
        CHECK (ano_curricular IS NULL OR ano_curricular >= 1),
    CONSTRAINT chk_curso_disciplina_periodo
        CHECK (periodo_curricular IS NULL OR periodo_curricular >= 1),
    CONSTRAINT fk_curso_disciplina_curso
        FOREIGN KEY (curso_id, instituicao_id)
        REFERENCES curso (id, instituicao_id),
    CONSTRAINT fk_curso_disciplina_disciplina
        FOREIGN KEY (disciplina_id, instituicao_id)
        REFERENCES disciplina (id, instituicao_id)
);
