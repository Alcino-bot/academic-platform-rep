ALTER TABLE utilizador
    ADD COLUMN tentativas_falhadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_ate TIMESTAMPTZ,
    ADD CONSTRAINT chk_utilizador_tentativas
        CHECK (tentativas_falhadas >= 0);

CREATE FUNCTION registar_falha_login(p_utilizador_id UUID, p_maximo INTEGER, p_duracao INTERVAL)
RETURNS TIMESTAMPTZ
LANGUAGE plpgsql
AS $$
DECLARE
    v_tentativas INTEGER;
    v_bloqueado_ate TIMESTAMPTZ;
BEGIN
    SELECT tentativas_falhadas, bloqueado_ate
      INTO v_tentativas, v_bloqueado_ate
      FROM utilizador
     WHERE id = p_utilizador_id
       FOR UPDATE;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;
    IF v_bloqueado_ate IS NOT NULL AND v_bloqueado_ate > now() THEN
        RETURN v_bloqueado_ate;
    END IF;
    IF v_bloqueado_ate IS NOT NULL THEN
        v_tentativas := 1;
    ELSE
        v_tentativas := v_tentativas + 1;
    END IF;
    IF v_tentativas >= p_maximo THEN
        v_bloqueado_ate := now() + p_duracao;
    ELSE
        v_bloqueado_ate := NULL;
    END IF;
    UPDATE utilizador
       SET tentativas_falhadas = v_tentativas,
           bloqueado_ate = v_bloqueado_ate
     WHERE id = p_utilizador_id;
    RETURN v_bloqueado_ate;
END;
$$;
