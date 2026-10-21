ALTER TABLE vinculo_institucional
    DROP CONSTRAINT chk_vinculo_institucional_estado;

ALTER TABLE vinculo_institucional
    ADD COLUMN decidido_por UUID,
    ADD COLUMN data_decisao TIMESTAMPTZ,
    ADD COLUMN motivo_rejeicao TEXT;

ALTER TABLE vinculo_institucional
    ADD CONSTRAINT fk_vinculo_institucional_decidido_por
        FOREIGN KEY (decidido_por) REFERENCES utilizador (id),
    ADD CONSTRAINT chk_vinculo_institucional_estado
        CHECK (estado IN ('PENDENTE', 'ACTIVO', 'SUSPENSO', 'TERMINADO', 'REJEITADO')),
    ADD CONSTRAINT chk_vinculo_institucional_rejeitado
        CHECK (estado <> 'REJEITADO'
            OR (decidido_por IS NOT NULL
                AND data_decisao IS NOT NULL
                AND motivo_rejeicao IS NOT NULL
                AND length(btrim(motivo_rejeicao)) > 0)),
    ADD CONSTRAINT chk_vinculo_institucional_motivo
        CHECK (motivo_rejeicao IS NULL OR estado = 'REJEITADO');
