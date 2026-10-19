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

CREATE FUNCTION pg_temp.booleano(p_teste text, p_sql text, p_esperado boolean)
RETURNS void LANGUAGE plpgsql AS $f$
DECLARE
    v_obtido boolean;
BEGIN
    EXECUTE p_sql INTO v_obtido;
    IF v_obtido IS DISTINCT FROM p_esperado THEN
        RAISE EXCEPTION 'TESTE FALHOU: % (esperado %, obtido %)', p_teste, p_esperado, v_obtido;
    END IF;
    RAISE NOTICE 'OK: % -> %', p_teste, v_obtido;
END;
$f$;

CREATE FUNCTION pg_temp.conjunto(p_teste text, p_sql text, p_colunas text, p_seleccao text, p_esperado text[])
RETURNS void LANGUAGE plpgsql AS $f$
DECLARE
    v_obtido text[];
    v_esperado text[];
BEGIN
    EXECUTE format('SELECT coalesce(array_agg(%s::text ORDER BY %s::text), ''{}''::text[]) FROM (%s) %s', p_seleccao, p_seleccao, p_sql, p_colunas) INTO v_obtido;
    SELECT coalesce(array_agg(e ORDER BY e), '{}'::text[]) INTO v_esperado FROM unnest(p_esperado) AS e;
    IF v_obtido IS DISTINCT FROM v_esperado THEN
        RAISE EXCEPTION 'TESTE FALHOU: % (esperado %, obtido %)', p_teste, v_esperado, v_obtido;
    END IF;
    RAISE NOTICE 'OK: % -> %', p_teste, v_obtido;
END;
$f$;

INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao)
VALUES ('10000000-0000-0000-0000-000000000001', 'University', 'APROVADA', now());

INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id, estado)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Academy A', 'AA', '10000000-0000-0000-0000-000000000001', 'ACTIVA'),
    ('40000000-0000-0000-0000-000000000002', 'Academy B', 'AB', '10000000-0000-0000-0000-000000000001', 'ACTIVA'),
    ('40000000-0000-0000-0000-000000000003', 'Academy C', 'AC', '10000000-0000-0000-0000-000000000001', 'PENDENTE');

INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'ana@example.org', 'Ana', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000002', 'bia@example.org', 'Bia', repeat('x', 60), 'SUSPENSO'),
    ('70000000-0000-0000-0000-000000000003', 'admin@example.org', 'Admin', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000004', 'carla@example.org', 'Carla', repeat('x', 60), 'ACTIVO');

INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado, data_inicio, data_fim)
VALUES
    ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 'DOCENTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', 'ESTUDANTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000005', '70000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 'FUNCIONARIO', 'SUSPENSO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000002', 'ESTUDANTE', 'TERMINADO', '2020-01-01', '2021-01-01');

INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id)
SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id FROM papel WHERE codigo = 'ESTUDANTE';
INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, estado, data_revogacao)
SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id, 'REVOGADO', now() FROM papel WHERE codigo = 'REVISOR_BIBLIOTECA';
INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id)
SELECT '80000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', id FROM papel WHERE codigo = 'DOCENTE';

INSERT INTO atribuicao_global (utilizador_id, papel_id)
SELECT '70000000-0000-0000-0000-000000000003', id FROM papel WHERE codigo = 'ADMIN_GERAL';
INSERT INTO atribuicao_global (utilizador_id, papel_id, estado, data_revogacao)
SELECT '70000000-0000-0000-0000-000000000001', id, 'REVOGADA', now() FROM papel WHERE codigo = 'ADMIN_GERAL';


SELECT pg_temp.booleano('procurar por email normalizado encontra a conta activa',
    $q$SELECT EXISTS (SELECT 1 FROM (SELECT id, email, nome_completo, palavra_passe_hash, estado, (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) AS bloqueada FROM utilizador WHERE lower(btrim(email)) = lower(btrim('  ANA@Example.ORG  '))) t WHERE t.id = '70000000-0000-0000-0000-000000000001'::uuid AND t.estado = 'ACTIVO' AND NOT t.bloqueada)$q$,
    true);

SELECT pg_temp.booleano('procurar por email inexistente nao devolve nada',
    $q$SELECT EXISTS (SELECT 1 FROM (SELECT id, email, nome_completo, palavra_passe_hash, estado, (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) AS bloqueada FROM utilizador WHERE lower(btrim(email)) = lower(btrim('ninguem@example.org'))) t)$q$,
    false);

SELECT pg_temp.booleano('procurar devolve o estado SUSPENSO da conta',
    $q$SELECT EXISTS (SELECT 1 FROM (SELECT id, email, nome_completo, palavra_passe_hash, estado, (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) AS bloqueada FROM utilizador WHERE lower(btrim(email)) = lower(btrim('bia@example.org'))) t WHERE t.estado = 'SUSPENSO')$q$,
    true);

SELECT pg_temp.executar($q$UPDATE utilizador SET bloqueado_ate = now() + interval '10 minutes' WHERE id = '70000000-0000-0000-0000-000000000004'$q$);

SELECT pg_temp.booleano('procurar marca a conta como bloqueada enquanto bloqueado_ate esta no futuro',
    $q$SELECT EXISTS (SELECT 1 FROM (SELECT id, email, nome_completo, palavra_passe_hash, estado, (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) AS bloqueada FROM utilizador WHERE lower(btrim(email)) = lower(btrim('carla@example.org'))) t WHERE t.bloqueada)$q$,
    true);

SELECT pg_temp.executar($q$UPDATE utilizador SET bloqueado_ate = now() - interval '10 minutes' WHERE id = '70000000-0000-0000-0000-000000000004'$q$);

SELECT pg_temp.booleano('procurar nao marca como bloqueada quando bloqueado_ate ja passou',
    $q$SELECT EXISTS (SELECT 1 FROM (SELECT id, email, nome_completo, palavra_passe_hash, estado, (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) AS bloqueada FROM utilizador WHERE lower(btrim(email)) = lower(btrim('carla@example.org'))) t WHERE t.bloqueada)$q$,
    false);

SELECT pg_temp.executar($q$UPDATE utilizador SET bloqueado_ate = NULL WHERE id = '70000000-0000-0000-0000-000000000004'$q$);

SELECT pg_temp.booleano('conta activa: utilizador ACTIVO',
    $q$SELECT EXISTS (SELECT 1 FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000001'::uuid AND estado = 'ACTIVO')$q$,
    true);

SELECT pg_temp.booleano('conta activa: utilizador SUSPENSO',
    $q$SELECT EXISTS (SELECT 1 FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000002'::uuid AND estado = 'ACTIVO')$q$,
    false);

SELECT pg_temp.booleano('conta activa: utilizador inexistente',
    $q$SELECT EXISTS (SELECT 1 FROM utilizador WHERE id = '99999999-9999-9999-9999-999999999999'::uuid AND estado = 'ACTIVO')$q$,
    false);

SELECT pg_temp.conjunto('instituicoes disponiveis: vinculos activos em instituicoes activas (A e B, nao a pendente C)',
    $q$SELECT i.id, i.nome, i.sigla FROM instituicao i WHERE i.estado = 'ACTIVA' AND EXISTS (SELECT 1 FROM vinculo_institucional v WHERE v.instituicao_id = i.id AND v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND v.estado = 'ACTIVO') ORDER BY i.nome$q$,
    't(a,b,c)', 'b', ARRAY['Academy A', 'Academy B']::text[]);

SELECT pg_temp.conjunto('instituicoes disponiveis: conta com vinculo activo apenas em A',
    $q$SELECT i.id, i.nome, i.sigla FROM instituicao i WHERE i.estado = 'ACTIVA' AND EXISTS (SELECT 1 FROM vinculo_institucional v WHERE v.instituicao_id = i.id AND v.utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND v.estado = 'ACTIVO') ORDER BY i.nome$q$,
    't(a,b,c)', 'b', ARRAY['Academy A']::text[]);

SELECT pg_temp.conjunto('instituicoes disponiveis: vinculo suspenso e terminado nao contam',
    $q$SELECT i.id, i.nome, i.sigla FROM instituicao i WHERE i.estado = 'ACTIVA' AND EXISTS (SELECT 1 FROM vinculo_institucional v WHERE v.instituicao_id = i.id AND v.utilizador_id = '70000000-0000-0000-0000-000000000003'::uuid AND v.estado = 'ACTIVO') ORDER BY i.nome$q$,
    't(a,b,c)', 'b', '{}'::text[]);

SELECT pg_temp.booleano('pode actuar: vinculo activo em instituicao activa',
    $q$SELECT EXISTS (SELECT 1 FROM instituicao i JOIN vinculo_institucional v ON v.instituicao_id = i.id WHERE i.id = '40000000-0000-0000-0000-000000000001'::uuid AND i.estado = 'ACTIVA' AND v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND v.estado = 'ACTIVO')$q$,
    true);

SELECT pg_temp.booleano('pode actuar: instituicao pendente nao conta',
    $q$SELECT EXISTS (SELECT 1 FROM instituicao i JOIN vinculo_institucional v ON v.instituicao_id = i.id WHERE i.id = '40000000-0000-0000-0000-000000000003'::uuid AND i.estado = 'ACTIVA' AND v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND v.estado = 'ACTIVO')$q$,
    false);

SELECT pg_temp.booleano('pode actuar: vinculo terminado nao conta',
    $q$SELECT EXISTS (SELECT 1 FROM instituicao i JOIN vinculo_institucional v ON v.instituicao_id = i.id WHERE i.id = '40000000-0000-0000-0000-000000000002'::uuid AND i.estado = 'ACTIVA' AND v.utilizador_id = '70000000-0000-0000-0000-000000000003'::uuid AND v.estado = 'ACTIVO')$q$,
    false);

SELECT pg_temp.booleano('pode actuar: vinculo suspenso nao conta',
    $q$SELECT EXISTS (SELECT 1 FROM instituicao i JOIN vinculo_institucional v ON v.instituicao_id = i.id WHERE i.id = '40000000-0000-0000-0000-000000000001'::uuid AND i.estado = 'ACTIVA' AND v.utilizador_id = '70000000-0000-0000-0000-000000000003'::uuid AND v.estado = 'ACTIVO')$q$,
    false);

SELECT pg_temp.booleano('pode actuar: utilizador sem vinculo nessa instituicao',
    $q$SELECT EXISTS (SELECT 1 FROM instituicao i JOIN vinculo_institucional v ON v.instituicao_id = i.id WHERE i.id = '40000000-0000-0000-0000-000000000002'::uuid AND i.estado = 'ACTIVA' AND v.utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND v.estado = 'ACTIVO')$q$,
    false);

SELECT pg_temp.conjunto('autoridades globais: ADMIN_GERAL activo',
    $q$SELECT 'PAPEL_GLOBAL_' || p.codigo FROM atribuicao_global a JOIN papel p ON p.id = a.papel_id WHERE a.utilizador_id = '70000000-0000-0000-0000-000000000003'::uuid AND a.estado = 'ACTIVA' AND p.estado = 'ACTIVO'$q$,
    't(x)', 'x', ARRAY['PAPEL_GLOBAL_ADMIN_GERAL']::text[]);

SELECT pg_temp.conjunto('autoridades globais: atribuicao revogada nao conta',
    $q$SELECT 'PAPEL_GLOBAL_' || p.codigo FROM atribuicao_global a JOIN papel p ON p.id = a.papel_id WHERE a.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND a.estado = 'ACTIVA' AND p.estado = 'ACTIVO'$q$,
    't(x)', 'x', '{}'::text[]);

SELECT pg_temp.conjunto('autoridades da instituicao A: papel activo mais tipo de vinculo (papel revogado fica de fora)',
    $q$SELECT 'PAPEL_' || p.codigo FROM vinculo_papel vp JOIN vinculo_institucional v ON v.id = vp.vinculo_id AND v.instituicao_id = vp.instituicao_id JOIN papel p ON p.id = vp.papel_id WHERE v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND vp.instituicao_id = '40000000-0000-0000-0000-000000000001'::uuid AND vp.estado = 'ACTIVO' AND v.estado = 'ACTIVO' AND p.estado = 'ACTIVO' UNION SELECT 'VINCULO_' || v.tipo_vinculo FROM vinculo_institucional v WHERE v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND v.instituicao_id = '40000000-0000-0000-0000-000000000001'::uuid AND v.estado = 'ACTIVO'$q$,
    't(x)', 'x', ARRAY['PAPEL_ESTUDANTE', 'VINCULO_ESTUDANTE']::text[]);

SELECT pg_temp.conjunto('autoridades da instituicao B: so as de B',
    $q$SELECT 'PAPEL_' || p.codigo FROM vinculo_papel vp JOIN vinculo_institucional v ON v.id = vp.vinculo_id AND v.instituicao_id = vp.instituicao_id JOIN papel p ON p.id = vp.papel_id WHERE v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND vp.instituicao_id = '40000000-0000-0000-0000-000000000002'::uuid AND vp.estado = 'ACTIVO' AND v.estado = 'ACTIVO' AND p.estado = 'ACTIVO' UNION SELECT 'VINCULO_' || v.tipo_vinculo FROM vinculo_institucional v WHERE v.utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND v.instituicao_id = '40000000-0000-0000-0000-000000000002'::uuid AND v.estado = 'ACTIVO'$q$,
    't(x)', 'x', ARRAY['PAPEL_DOCENTE', 'VINCULO_DOCENTE']::text[]);

SELECT pg_temp.conjunto('autoridades da instituicao A para vinculo suspenso: nenhuma',
    $q$SELECT 'PAPEL_' || p.codigo FROM vinculo_papel vp JOIN vinculo_institucional v ON v.id = vp.vinculo_id AND v.instituicao_id = vp.instituicao_id JOIN papel p ON p.id = vp.papel_id WHERE v.utilizador_id = '70000000-0000-0000-0000-000000000003'::uuid AND vp.instituicao_id = '40000000-0000-0000-0000-000000000001'::uuid AND vp.estado = 'ACTIVO' AND v.estado = 'ACTIVO' AND p.estado = 'ACTIVO' UNION SELECT 'VINCULO_' || v.tipo_vinculo FROM vinculo_institucional v WHERE v.utilizador_id = '70000000-0000-0000-0000-000000000003'::uuid AND v.instituicao_id = '40000000-0000-0000-0000-000000000001'::uuid AND v.estado = 'ACTIVO'$q$,
    't(x)', 'x', '{}'::text[]);

SELECT pg_temp.afirmar('conta nova: zero tentativas e sem bloqueio', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 0 AND (SELECT bloqueado_ate IS  NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0))$q$);

SELECT pg_temp.afirmar('falha 1 de 5: contador 1 e ainda sem bloqueio', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 1 AND (SELECT bloqueado_ate IS  NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0))$q$);

SELECT pg_temp.afirmar('falha 2 de 5: contador 2 e ainda sem bloqueio', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 2 AND (SELECT bloqueado_ate IS  NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0))$q$);

SELECT pg_temp.afirmar('falha 3 de 5: contador 3 e ainda sem bloqueio', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 3 AND (SELECT bloqueado_ate IS  NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0))$q$);

SELECT pg_temp.afirmar('falha 4 de 5: contador 4 e ainda sem bloqueio', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 4 AND (SELECT bloqueado_ate IS  NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0))$q$);

SELECT pg_temp.afirmar('falha 5 de 5: contador 5 e conta bloqueada', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 5 AND (SELECT bloqueado_ate IS NOT NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.afirmar('bloqueio dura cerca de 15 minutos', (SELECT bloqueado_ate > now() + interval '14 minutes' AND bloqueado_ate < now() + interval '16 minutes' FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.afirmar('falha durante o bloqueio devolve o mesmo instante', (SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0)) = (SELECT bloqueado_ate FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid)));

SELECT pg_temp.afirmar('falha durante o bloqueio nao aumenta o contador nem prolonga o bloqueio', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 5 AND (SELECT bloqueado_ate IS NOT NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.afirmar('falha durante o bloqueio nao prolonga o instante', (SELECT bloqueado_ate < now() + interval '16 minutes' FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$UPDATE utilizador SET bloqueado_ate = now() - interval '1 minute' WHERE id = '70000000-0000-0000-0000-000000000004'$q$);

SELECT pg_temp.executar($q$SELECT registar_falha_login('70000000-0000-0000-0000-000000000004'::uuid, 5, make_interval(secs => 900.0))$q$);

SELECT pg_temp.afirmar('falha depois de o bloqueio expirar recomeca a contagem em 1 sem bloquear', (SELECT tentativas_falhadas FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid) = 1 AND (SELECT bloqueado_ate IS  NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.executar($q$UPDATE utilizador SET tentativas_falhadas = 0, bloqueado_ate = NULL, ultimo_acesso = now() WHERE id = '70000000-0000-0000-0000-000000000004'::uuid$q$);

SELECT pg_temp.afirmar('login valido repoe contador e bloqueio e regista o ultimo acesso', (SELECT tentativas_falhadas = 0 AND bloqueado_ate IS NULL AND ultimo_acesso IS NOT NULL FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000004'::uuid));

SELECT pg_temp.afirmar('falha para utilizador inexistente devolve NULL sem erro', (SELECT registar_falha_login('99999999-9999-9999-9999-999999999999'::uuid, 5, make_interval(secs => 900.0)) IS NULL));

SELECT pg_temp.esperar_falha('contador de tentativas nao pode ser negativo',
    $q$UPDATE utilizador SET tentativas_falhadas = -1 WHERE id = '70000000-0000-0000-0000-000000000004'$q$,
    'chk_utilizador_tentativas');

ROLLBACK;
