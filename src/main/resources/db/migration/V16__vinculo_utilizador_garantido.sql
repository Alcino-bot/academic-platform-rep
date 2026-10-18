ALTER TABLE vinculo_institucional
    ADD CONSTRAINT uq_vinculo_institucional_id_instituicao_utilizador
    UNIQUE (id, instituicao_id, utilizador_id);
