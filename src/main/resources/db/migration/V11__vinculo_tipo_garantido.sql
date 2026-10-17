ALTER TABLE vinculo_institucional
    ADD CONSTRAINT uq_vinculo_institucional_id_instituicao_tipo
    UNIQUE (id, instituicao_id, tipo_vinculo);
