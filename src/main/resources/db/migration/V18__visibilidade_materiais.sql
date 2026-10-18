CREATE FUNCTION materiais_visiveis(p_utilizador_id UUID)
RETURNS SETOF UUID
LANGUAGE sql
STABLE
AS $$
    SELECT m.id
    FROM material m
    WHERE m.estado = 'PUBLICADO'
      AND EXISTS (
          SELECT 1
          FROM utilizador u
          WHERE u.id = p_utilizador_id
            AND u.estado = 'ACTIVO'
      )
      AND (
          m.ambito = 'BIBLIOTECA'
          OR EXISTS (
              SELECT 1
              FROM vinculo_institucional v
              WHERE v.utilizador_id = p_utilizador_id
                AND v.instituicao_id = m.instituicao_id
                AND v.estado = 'ACTIVO'
          )
      )
$$;
