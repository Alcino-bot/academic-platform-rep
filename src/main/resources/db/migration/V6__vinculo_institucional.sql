CREATE TABLE vinculo_institucional (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    utilizador_id UUID NOT NULL REFERENCES utilizador (id),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    tipo_vinculo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    numero_identificacao VARCHAR(50),
    data_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    data_fim DATE,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vinculo_institucional_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_vinculo_institucional_tipo
        CHECK (tipo_vinculo IN ('ESTUDANTE', 'DOCENTE', 'FUNCIONARIO', 'OUTRO')),
    CONSTRAINT chk_vinculo_institucional_estado
        CHECK (estado IN ('PENDENTE', 'ACTIVO', 'SUSPENSO', 'TERMINADO')),
    CONSTRAINT chk_vinculo_institucional_datas
        CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT chk_vinculo_institucional_terminado
        CHECK (estado <> 'TERMINADO' OR data_fim IS NOT NULL)
);

CREATE UNIQUE INDEX uq_vinculo_institucional_pessoa
    ON vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo);
