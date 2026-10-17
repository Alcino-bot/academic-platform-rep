CREATE TABLE matricula (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    vinculo_id UUID NOT NULL,
    tipo_vinculo VARCHAR(20) NOT NULL DEFAULT 'ESTUDANTE',
    curso_id UUID NOT NULL,
    ano_lectivo_id UUID NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    data_matricula DATE NOT NULL DEFAULT CURRENT_DATE,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_matricula_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT uq_matricula UNIQUE (vinculo_id, curso_id, ano_lectivo_id),
    CONSTRAINT chk_matricula_tipo
        CHECK (tipo_vinculo = 'ESTUDANTE'),
    CONSTRAINT chk_matricula_estado
        CHECK (estado IN ('ACTIVA', 'SUSPENSA', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT fk_matricula_vinculo
        FOREIGN KEY (vinculo_id, instituicao_id, tipo_vinculo)
        REFERENCES vinculo_institucional (id, instituicao_id, tipo_vinculo),
    CONSTRAINT fk_matricula_curso
        FOREIGN KEY (curso_id, instituicao_id)
        REFERENCES curso (id, instituicao_id),
    CONSTRAINT fk_matricula_ano_lectivo
        FOREIGN KEY (ano_lectivo_id, instituicao_id)
        REFERENCES ano_lectivo (id, instituicao_id)
);

CREATE TABLE inscricao (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    matricula_id UUID NOT NULL,
    turma_id UUID NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    data_inscricao DATE NOT NULL DEFAULT CURRENT_DATE,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inscricao UNIQUE (matricula_id, turma_id),
    CONSTRAINT chk_inscricao_estado
        CHECK (estado IN ('ACTIVA', 'CANCELADA', 'CONCLUIDA')),
    CONSTRAINT fk_inscricao_matricula
        FOREIGN KEY (matricula_id, instituicao_id)
        REFERENCES matricula (id, instituicao_id),
    CONSTRAINT fk_inscricao_turma
        FOREIGN KEY (turma_id, instituicao_id)
        REFERENCES turma (id, instituicao_id)
);
