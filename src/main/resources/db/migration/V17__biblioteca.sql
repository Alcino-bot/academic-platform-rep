CREATE TABLE material (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    titulo VARCHAR(300) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(20) NOT NULL,
    ligacao_externa VARCHAR(2000),
    ambito VARCHAR(20) NOT NULL DEFAULT 'INSTITUCIONAL',
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    submetido_por_vinculo_id UUID NOT NULL,
    submetido_por_utilizador_id UUID NOT NULL,
    data_submissao TIMESTAMPTZ NOT NULL DEFAULT now(),
    revisto_por_vinculo_id UUID,
    revisto_por_utilizador_id UUID,
    data_revisao TIMESTAMPTZ,
    motivo_rejeicao TEXT,
    retirado_por UUID REFERENCES utilizador (id),
    data_retirada TIMESTAMPTZ,
    CONSTRAINT uq_material_id_instituicao UNIQUE (id, instituicao_id),
    CONSTRAINT chk_material_titulo
        CHECK (length(btrim(titulo)) > 0),
    CONSTRAINT chk_material_tipo
        CHECK (tipo IN ('DOCUMENTO', 'APRESENTACAO', 'VIDEO', 'AUDIO', 'LIGACAO', 'OUTRO')),
    CONSTRAINT chk_material_ambito
        CHECK (ambito IN ('INSTITUCIONAL', 'BIBLIOTECA')),
    CONSTRAINT chk_material_estado
        CHECK (estado IN ('PENDENTE', 'PUBLICADO', 'REJEITADO', 'RETIRADO')),
    CONSTRAINT chk_material_ligacao
        CHECK ((tipo = 'LIGACAO' AND COALESCE(ligacao_externa ~ '^https?://.+', false))
            OR (tipo <> 'LIGACAO' AND ligacao_externa IS NULL)),
    CONSTRAINT chk_material_revisao_par
        CHECK ((revisto_por_vinculo_id IS NULL) = (revisto_por_utilizador_id IS NULL)),
    CONSTRAINT chk_material_autorevisao
        CHECK (revisto_por_utilizador_id IS DISTINCT FROM submetido_por_utilizador_id),
    CONSTRAINT chk_material_estado_campos
        CHECK (
            (estado = 'PENDENTE'
                AND revisto_por_utilizador_id IS NULL AND data_revisao IS NULL
                AND motivo_rejeicao IS NULL
                AND retirado_por IS NULL AND data_retirada IS NULL)
            OR (estado = 'PUBLICADO'
                AND revisto_por_utilizador_id IS NOT NULL AND data_revisao IS NOT NULL
                AND motivo_rejeicao IS NULL
                AND retirado_por IS NULL AND data_retirada IS NULL)
            OR (estado = 'REJEITADO'
                AND revisto_por_utilizador_id IS NOT NULL AND data_revisao IS NOT NULL
                AND motivo_rejeicao IS NOT NULL AND length(btrim(motivo_rejeicao)) > 0
                AND retirado_por IS NULL AND data_retirada IS NULL)
            OR (estado = 'RETIRADO'
                AND revisto_por_utilizador_id IS NOT NULL AND data_revisao IS NOT NULL
                AND motivo_rejeicao IS NULL
                AND retirado_por IS NOT NULL AND data_retirada IS NOT NULL)
        ),
    CONSTRAINT fk_material_submissor
        FOREIGN KEY (submetido_por_vinculo_id, instituicao_id, submetido_por_utilizador_id)
        REFERENCES vinculo_institucional (id, instituicao_id, utilizador_id),
    CONSTRAINT fk_material_revisor
        FOREIGN KEY (revisto_por_vinculo_id, instituicao_id, revisto_por_utilizador_id)
        REFERENCES vinculo_institucional (id, instituicao_id, utilizador_id)
);

CREATE INDEX ix_material_instituicao_estado
    ON material (instituicao_id, estado);

CREATE INDEX ix_material_ambito_estado
    ON material (ambito, estado);

CREATE TABLE ficheiro_material (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    material_id UUID NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 1,
    nome_original VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    hash_sha256 CHAR(64) NOT NULL,
    chave_armazenamento VARCHAR(500) NOT NULL,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ficheiro_material_ordem UNIQUE (material_id, ordem),
    CONSTRAINT uq_ficheiro_material_chave UNIQUE (chave_armazenamento),
    CONSTRAINT chk_ficheiro_material_ordem
        CHECK (ordem >= 1),
    CONSTRAINT chk_ficheiro_material_nome
        CHECK (length(btrim(nome_original)) > 0),
    CONSTRAINT chk_ficheiro_material_tamanho
        CHECK (tamanho_bytes > 0),
    CONSTRAINT chk_ficheiro_material_hash
        CHECK (hash_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_ficheiro_material_chave
        CHECK (length(btrim(chave_armazenamento)) > 0),
    CONSTRAINT fk_ficheiro_material_material
        FOREIGN KEY (material_id, instituicao_id)
        REFERENCES material (id, instituicao_id)
);

CREATE INDEX ix_ficheiro_material_hash
    ON ficheiro_material (hash_sha256);

CREATE TABLE material_disciplina (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    instituicao_id UUID NOT NULL REFERENCES instituicao (id),
    material_id UUID NOT NULL,
    disciplina_id UUID NOT NULL,
    CONSTRAINT uq_material_disciplina UNIQUE (material_id, disciplina_id),
    CONSTRAINT fk_material_disciplina_material
        FOREIGN KEY (material_id, instituicao_id)
        REFERENCES material (id, instituicao_id),
    CONSTRAINT fk_material_disciplina_disciplina
        FOREIGN KEY (disciplina_id, instituicao_id)
        REFERENCES disciplina (id, instituicao_id)
);
