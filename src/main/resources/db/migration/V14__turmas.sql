CREATE TABLE turma (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    disciplina_id UUID NOT NULL,
    periodo_id UUID NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    capacidade INTEGER,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_turma_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_turma_codigo
        CHECK (length(btrim(codigo)) > 0),
    CONSTRAINT chk_turma_capacidade
        CHECK (capacidade IS NULL OR capacidade > 0),
    CONSTRAINT chk_turma_estado
        CHECK (estado IN ('ABERTA', 'FECHADA', 'CANCELADA')),
    CONSTRAINT fk_turma_disciplina
        FOREIGN KEY (disciplina_id, instituicao_id)
        REFERENCES disciplina (id, instituicao_id),
    CONSTRAINT fk_turma_periodo
        FOREIGN KEY (periodo_id, instituicao_id)
        REFERENCES periodo (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_turma_codigo
    ON turma (disciplina_id, periodo_id, lower(btrim(codigo)));

CREATE TABLE docente_turma (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    turma_id UUID NOT NULL,
    vinculo_id UUID NOT NULL,
    tipo_vinculo VARCHAR(20) NOT NULL DEFAULT 'DOCENTE',
    funcao VARCHAR(20) NOT NULL DEFAULT 'RESPONSAVEL',
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_docente_turma UNIQUE (turma_id, vinculo_id),
    CONSTRAINT chk_docente_turma_tipo
        CHECK (tipo_vinculo = 'DOCENTE'),
    CONSTRAINT chk_docente_turma_funcao
        CHECK (funcao IN ('RESPONSAVEL', 'AUXILIAR')),
    CONSTRAINT fk_docente_turma_turma
        FOREIGN KEY (turma_id, instituicao_id)
        REFERENCES turma (id, instituicao_id),
    CONSTRAINT fk_docente_turma_vinculo
        FOREIGN KEY (vinculo_id, instituicao_id, tipo_vinculo)
        REFERENCES vinculo_institucional (id, instituicao_id, tipo_vinculo)
);
