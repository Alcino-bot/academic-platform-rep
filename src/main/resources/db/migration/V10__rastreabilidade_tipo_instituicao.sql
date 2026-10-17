ALTER TABLE tipo_instituicao
    ADD COLUMN proposto_por UUID REFERENCES utilizador (id),
    ADD COLUMN decidido_por UUID REFERENCES utilizador (id),
    ADD CONSTRAINT chk_tipo_instituicao_decisao
        CHECK (decidido_por IS NULL OR estado <> 'PENDENTE');
