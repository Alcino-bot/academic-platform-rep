BEGIN;

CREATE FUNCTION pg_temp.esperar_falha(p_teste text, p_sql text, p_restricao text)
RETURNS void LANGUAGE plpgsql AS $f$
DECLARE
    v_falhou boolean := false;
    v_restricao text;
BEGIN
    BEGIN
        EXECUTE p_sql;
    EXCEPTION WHEN OTHERS THEN
        v_falhou := true;
        GET STACKED DIAGNOSTICS v_restricao = CONSTRAINT_NAME;
    END;
    IF NOT v_falhou THEN
        RAISE EXCEPTION 'TESTE FALHOU: % (a instrucao foi aceite)', p_teste;
    END IF;
    IF v_restricao IS DISTINCT FROM p_restricao THEN
        RAISE EXCEPTION 'TESTE FALHOU: % (esperado %, obtido %)', p_teste, p_restricao, v_restricao;
    END IF;
    RAISE NOTICE 'OK: % -> %', p_teste, v_restricao;
END;
$f$;

CREATE FUNCTION pg_temp.esperar_sucesso(p_teste text, p_sql text)
RETURNS void
LANGUAGE plpgsql
AS $f$
BEGIN
    EXECUTE p_sql;
    RAISE NOTICE 'OK: % -> aceite', p_teste;
EXCEPTION WHEN OTHERS THEN
    IF SQLSTATE = 'P0001' THEN
        RAISE;
    END IF;
    RAISE EXCEPTION 'TESTE FALHOU: % (erro inesperado: %)', p_teste, SQLERRM;
END;
$f$;

CREATE FUNCTION pg_temp.executar(p_sql text)
RETURNS void LANGUAGE plpgsql AS $f$
BEGIN
    EXECUTE p_sql;
END;
$f$;

CREATE FUNCTION pg_temp.afirmar(p_teste text, p_condicao boolean)
RETURNS void LANGUAGE plpgsql AS $f$
BEGIN
    IF p_condicao IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'TESTE FALHOU: %', p_teste;
    END IF;
    RAISE NOTICE 'OK: %', p_teste;
END;
$f$;

CREATE FUNCTION pg_temp.linhas(p_sql text)
RETURNS integer LANGUAGE plpgsql AS $f$
DECLARE
    n integer;
BEGIN
    EXECUTE p_sql;
    GET DIAGNOSTICS n = ROW_COUNT;
    RETURN n;
END;
$f$;

INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao)
VALUES ('10000000-0000-0000-0000-000000000001', 'University', 'APROVADA', now());

INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Academy A', 'AA', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', 'Academy B', 'AB', '10000000-0000-0000-0000-000000000001');

INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'ana@example.org', 'Ana Test', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000002', 'admin@example.org', 'Admin Test', repeat('x', 60), 'ACTIVO');

SELECT pg_temp.esperar_sucesso('vinculo PENDENTE inicial',
    $q$INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo) VALUES ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE')$q$);

SELECT pg_temp.esperar_falha('REJEITADO sem decisao nem motivo',
    $q$UPDATE vinculo_institucional SET estado = 'REJEITADO' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'chk_vinculo_institucional_rejeitado');

SELECT pg_temp.esperar_falha('REJEITADO sem decidido_por',
    $q$UPDATE vinculo_institucional SET estado = 'REJEITADO', data_decisao = now(), motivo_rejeicao = 'Documentos em falta' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'chk_vinculo_institucional_rejeitado');

SELECT pg_temp.esperar_falha('REJEITADO sem data_decisao',
    $q$UPDATE vinculo_institucional SET estado = 'REJEITADO', decidido_por = '70000000-0000-0000-0000-000000000002', motivo_rejeicao = 'Documentos em falta' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'chk_vinculo_institucional_rejeitado');

SELECT pg_temp.esperar_falha('REJEITADO com motivo em branco',
    $q$UPDATE vinculo_institucional SET estado = 'REJEITADO', decidido_por = '70000000-0000-0000-0000-000000000002', data_decisao = now(), motivo_rejeicao = '   ' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'chk_vinculo_institucional_rejeitado');

SELECT pg_temp.esperar_falha('motivo de rejeicao num vinculo que nao esta REJEITADO',
    $q$UPDATE vinculo_institucional SET motivo_rejeicao = 'Documentos em falta' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'chk_vinculo_institucional_motivo');

SELECT pg_temp.esperar_falha('decidido_por de utilizador inexistente',
    $q$UPDATE vinculo_institucional SET decidido_por = '99999999-9999-9999-9999-999999999999' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'fk_vinculo_institucional_decidido_por');

SELECT pg_temp.esperar_falha('vinculo continua a recusar estado invalido',
    $q$UPDATE vinculo_institucional SET estado = 'INVALIDO' WHERE id = '80000000-0000-0000-0000-000000000001'$q$,
    'chk_vinculo_institucional_estado');

SELECT pg_temp.esperar_sucesso('vinculo REJEITADO com decisao e motivo',
    $q$UPDATE vinculo_institucional SET estado = 'REJEITADO', decidido_por = '70000000-0000-0000-0000-000000000002', data_decisao = now(), motivo_rejeicao = 'Documentos em falta' WHERE id = '80000000-0000-0000-0000-000000000001'$q$);

SELECT pg_temp.afirmar('novo pedido reabre o vinculo rejeitado',
    pg_temp.linhas($q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE') ON CONFLICT (utilizador_id, instituicao_id, tipo_vinculo) DO UPDATE SET estado = 'PENDENTE', data_inicio = CURRENT_DATE, data_fim = NULL, decidido_por = NULL, data_decisao = NULL, motivo_rejeicao = NULL WHERE vinculo_institucional.estado IN ('REJEITADO', 'TERMINADO')$q$) = 1);

SELECT pg_temp.afirmar('vinculo reaberto fica PENDENTE e sem decisao',
    (SELECT estado = 'PENDENTE' AND decidido_por IS NULL AND data_decisao IS NULL AND motivo_rejeicao IS NULL FROM vinculo_institucional WHERE id = '80000000-0000-0000-0000-000000000001'));

SELECT pg_temp.executar($q$UPDATE vinculo_institucional SET estado = 'ACTIVO' WHERE id = '80000000-0000-0000-0000-000000000001'$q$);

SELECT pg_temp.afirmar('novo pedido sobre vinculo ACTIVO nao altera nada',
    pg_temp.linhas($q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE') ON CONFLICT (utilizador_id, instituicao_id, tipo_vinculo) DO UPDATE SET estado = 'PENDENTE', data_inicio = CURRENT_DATE, data_fim = NULL, decidido_por = NULL, data_decisao = NULL, motivo_rejeicao = NULL WHERE vinculo_institucional.estado IN ('REJEITADO', 'TERMINADO')$q$) = 0);

SELECT pg_temp.afirmar('vinculo ACTIVO continua ACTIVO',
    (SELECT estado FROM vinculo_institucional WHERE id = '80000000-0000-0000-0000-000000000001') = 'ACTIVO');

SELECT pg_temp.executar($q$UPDATE vinculo_institucional SET estado = 'TERMINADO', data_fim = CURRENT_DATE WHERE id = '80000000-0000-0000-0000-000000000001'$q$);

SELECT pg_temp.afirmar('novo pedido reabre um vinculo TERMINADO',
    pg_temp.linhas($q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE') ON CONFLICT (utilizador_id, instituicao_id, tipo_vinculo) DO UPDATE SET estado = 'PENDENTE', data_inicio = CURRENT_DATE, data_fim = NULL, decidido_por = NULL, data_decisao = NULL, motivo_rejeicao = NULL WHERE vinculo_institucional.estado IN ('REJEITADO', 'TERMINADO')$q$) = 1);

SELECT pg_temp.afirmar('pedido noutra instituicao cria um vinculo proprio',
    pg_temp.linhas($q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 'DOCENTE') ON CONFLICT (utilizador_id, instituicao_id, tipo_vinculo) DO UPDATE SET estado = 'PENDENTE' WHERE vinculo_institucional.estado IN ('REJEITADO', 'TERMINADO')$q$) = 1);

SELECT pg_temp.afirmar('a pessoa tem um vinculo DOCENTE em cada instituicao',
    (SELECT count(*) FROM vinculo_institucional WHERE utilizador_id = '70000000-0000-0000-0000-000000000001' AND tipo_vinculo = 'DOCENTE') = 2);

ROLLBACK;
