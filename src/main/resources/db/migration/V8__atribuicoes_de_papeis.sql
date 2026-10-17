CREATE TABLE vinculo_papel (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    vinculo_id UUID NOT NULL,
    instituicao_id UUID NOT NULL,
    papel_id UUID NOT NULL,
    ambito VARCHAR(20) NOT NULL DEFAULT 'INSTITUCIONAL',
    unidade_id UUID,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    atribuido_por UUID REFERENCES utilizador (id),
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_revogacao TIMESTAMPTZ,
    CONSTRAINT chk_vinculo_papel_ambito
        CHECK (ambito = 'INSTITUCIONAL'),
    CONSTRAINT chk_vinculo_papel_estado
        CHECK (estado IN ('ACTIVO', 'REVOGADO')),
    CONSTRAINT chk_vinculo_papel_revogacao
        CHECK ((estado = 'REVOGADO' AND data_revogacao IS NOT NULL)
            OR (estado = 'ACTIVO' AND data_revogacao IS NULL)),
    CONSTRAINT fk_vinculo_papel_vinculo
        FOREIGN KEY (vinculo_id, instituicao_id)
        REFERENCES vinculo_institucional (id, instituicao_id),
    CONSTRAINT fk_vinculo_papel_papel
        FOREIGN KEY (papel_id, ambito)
        REFERENCES papel (id, ambito),
    CONSTRAINT fk_vinculo_papel_unidade
        FOREIGN KEY (unidade_id, instituicao_id)
        REFERENCES unidade_organizacional (id, instituicao_id)
);

CREATE UNIQUE INDEX uq_vinculo_papel_activo
    ON vinculo_papel (vinculo_id, papel_id, unidade_id)
    NULLS NOT DISTINCT
    WHERE estado = 'ACTIVO';

CREATE TABLE atribuicao_global (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    utilizador_id UUID NOT NULL REFERENCES utilizador (id),
    papel_id UUID NOT NULL,
    ambito VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    atribuido_por UUID REFERENCES utilizador (id),
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_revogacao TIMESTAMPTZ,
    CONSTRAINT chk_atribuicao_global_ambito
        CHECK (ambito = 'GLOBAL'),
    CONSTRAINT chk_atribuicao_global_estado
        CHECK (estado IN ('ACTIVA', 'REVOGADA')),
    CONSTRAINT chk_atribuicao_global_revogacao
        CHECK ((estado = 'REVOGADA' AND data_revogacao IS NOT NULL)
            OR (estado = 'ACTIVA' AND data_revogacao IS NULL)),
    CONSTRAINT fk_atribuicao_global_papel
        FOREIGN KEY (papel_id, ambito)
        REFERENCES papel (id, ambito)
);

CREATE UNIQUE INDEX uq_atribuicao_global_activa
    ON atribuicao_global (utilizador_id, papel_id)
    WHERE estado = 'ACTIVA';
