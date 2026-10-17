BEGIN;

CREATE FUNCTION pg_temp.esperar_falha(p_teste text, p_sql text, p_restricao text)
RETURNS void
LANGUAGE plpgsql
AS $f$
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

INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao)
VALUES ('10000000-0000-0000-0000-000000000001', 'University', 'APROVADA', now());

INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Academy A', 'AA', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', 'Academy B', 'AB', '10000000-0000-0000-0000-000000000001');

INSERT INTO tipo_unidade (id, instituicao_id, nome, nivel)
VALUES
    ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Faculty', 1),
    ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'Faculty', 1);

INSERT INTO unidade_organizacional (id, instituicao_id, tipo_unidade_id, nome)
VALUES
    ('60000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Unit A'),
    ('60000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', 'Unit B');

INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'ana@example.org', 'Ana Test', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000002', 'admin@example.org', 'Admin Test', repeat('x', 60), 'ACTIVO');

SELECT pg_temp.esperar_falha('utilizador email duplicado normalizado',
    $q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES ('  ANA@Example.ORG  ', 'Dup', repeat('x', 60))$q$,
    'uq_utilizador_email');
SELECT pg_temp.esperar_falha('utilizador email sem arroba',
    $q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES ('semarroba.example.org', 'N', repeat('x', 60))$q$,
    'chk_utilizador_email');
SELECT pg_temp.esperar_falha('utilizador estado invalido',
    $q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash, estado) VALUES ('e1@example.org', 'N', repeat('x', 60), 'INVALIDO')$q$,
    'chk_utilizador_estado');
SELECT pg_temp.esperar_falha('utilizador nome vazio',
    $q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES ('e2@example.org', '   ', repeat('x', 60))$q$,
    'chk_utilizador_nome');
SELECT pg_temp.esperar_falha('utilizador hash curto',
    $q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES ('e3@example.org', 'N', '1234')$q$,
    'chk_utilizador_hash');

SELECT pg_temp.esperar_sucesso('vinculo mesmo utilizador na instituicao A',
    $q$INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE', 'ACTIVO')$q$);
SELECT pg_temp.esperar_sucesso('vinculo mesmo utilizador na instituicao B',
    $q$INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES ('80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 'DOCENTE', 'ACTIVO')$q$);
SELECT pg_temp.esperar_sucesso('vinculo outro tipo na mesma instituicao',
    $q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE')$q$);
SELECT pg_temp.esperar_falha('vinculo duplicado utilizador instituicao tipo',
    $q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE')$q$,
    'uq_vinculo_institucional_pessoa');
SELECT pg_temp.esperar_falha('vinculo tipo invalido',
    $q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo) VALUES ('70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'ALUNO')$q$,
    'chk_vinculo_institucional_tipo');
SELECT pg_temp.esperar_falha('vinculo estado invalido',
    $q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES ('70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'DOCENTE', 'INVALIDO')$q$,
    'chk_vinculo_institucional_estado');
SELECT pg_temp.esperar_falha('vinculo data_fim anterior a data_inicio',
    $q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo, data_inicio, data_fim) VALUES ('70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'DOCENTE', '2026-01-10', '2026-01-01')$q$,
    'chk_vinculo_institucional_datas');
SELECT pg_temp.esperar_falha('vinculo TERMINADO sem data_fim',
    $q$INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo, estado) VALUES ('70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'DOCENTE', 'TERMINADO')$q$,
    'chk_vinculo_institucional_terminado');

SELECT pg_temp.esperar_falha('papel codigo duplicado',
    $q$INSERT INTO papel (codigo, nome, ambito) VALUES ('DOCENTE', 'Outro docente', 'INSTITUCIONAL')$q$,
    'uq_papel_codigo');
SELECT pg_temp.esperar_falha('papel codigo em minusculas',
    $q$INSERT INTO papel (codigo, nome, ambito) VALUES ('docente_x', 'Docente X', 'INSTITUCIONAL')$q$,
    'chk_papel_codigo');
SELECT pg_temp.esperar_falha('papel ambito invalido',
    $q$INSERT INTO papel (codigo, nome, ambito) VALUES ('PAPEL_X', 'Papel X', 'LOCAL')$q$,
    'chk_papel_ambito');
SELECT pg_temp.esperar_sucesso('permissao nova',
    $q$INSERT INTO permissao (codigo, descricao) VALUES ('BIBLIOTECA_REVER', 'Rever materiais')$q$);
SELECT pg_temp.esperar_falha('permissao codigo duplicado',
    $q$INSERT INTO permissao (codigo) VALUES ('BIBLIOTECA_REVER')$q$,
    'uq_permissao_codigo');
SELECT pg_temp.esperar_sucesso('papel_permissao associacao',
    $q$INSERT INTO papel_permissao (papel_id, permissao_id) SELECT p.id, m.id FROM papel p, permissao m WHERE p.codigo = 'REVISOR_BIBLIOTECA' AND m.codigo = 'BIBLIOTECA_REVER'$q$);

DO $t$
DECLARE
    v_total integer;
    v_globais integer;
    v_nome text;
BEGIN
    SELECT count(*) INTO v_total FROM papel
    WHERE codigo IN ('ADMIN_GERAL', 'ADMIN_INSTITUICAO', 'DOCENTE', 'ESTUDANTE', 'REVISOR_BIBLIOTECA');
    IF v_total <> 5 THEN
        RAISE EXCEPTION 'TESTE FALHOU: papeis iniciais (esperado 5, obtido %)', v_total;
    END IF;
    SELECT count(*) INTO v_globais FROM papel WHERE ambito = 'GLOBAL';
    IF v_globais <> 1 OR NOT EXISTS (SELECT 1 FROM papel WHERE codigo = 'ADMIN_GERAL' AND ambito = 'GLOBAL') THEN
        RAISE EXCEPTION 'TESTE FALHOU: apenas ADMIN_GERAL deve ser GLOBAL';
    END IF;
    SELECT nome INTO v_nome FROM papel WHERE codigo = 'ADMIN_INSTITUICAO';
    IF v_nome IS DISTINCT FROM 'Administrador da institui' || chr(231) || chr(227) || 'o' THEN
        RAISE EXCEPTION 'TESTE FALHOU: acentos do papel ADMIN_INSTITUICAO (obtido %)', v_nome;
    END IF;
    RAISE NOTICE 'OK: papeis iniciais (5 papeis, ADMIN_GERAL global, acentos correctos)';
END;
$t$;

SELECT pg_temp.esperar_sucesso('vinculo_papel DOCENTE sem unidade',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id FROM papel WHERE codigo = 'DOCENTE'$q$);
SELECT pg_temp.esperar_sucesso('vinculo_papel DOCENTE limitado a unidade da propria instituicao',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, unidade_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id, '60000000-0000-0000-0000-000000000001' FROM papel WHERE codigo = 'DOCENTE'$q$);
SELECT pg_temp.esperar_falha('vinculo_papel papel GLOBAL atribuido a vinculo',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id FROM papel WHERE codigo = 'ADMIN_GERAL'$q$,
    'fk_vinculo_papel_papel');
SELECT pg_temp.esperar_falha('vinculo_papel instituicao diferente da do vinculo',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', id FROM papel WHERE codigo = 'ESTUDANTE'$q$,
    'fk_vinculo_papel_vinculo');
SELECT pg_temp.esperar_falha('vinculo_papel unidade de outra instituicao',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, unidade_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id, '60000000-0000-0000-0000-000000000002' FROM papel WHERE codigo = 'ESTUDANTE'$q$,
    'fk_vinculo_papel_unidade');
SELECT pg_temp.esperar_falha('vinculo_papel activo duplicado sem unidade',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id FROM papel WHERE codigo = 'DOCENTE'$q$,
    'uq_vinculo_papel_activo');
SELECT pg_temp.esperar_falha('vinculo_papel activo duplicado com unidade',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, unidade_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id, '60000000-0000-0000-0000-000000000001' FROM papel WHERE codigo = 'DOCENTE'$q$,
    'uq_vinculo_papel_activo');
SELECT pg_temp.esperar_falha('vinculo_papel REVOGADO sem data_revogacao',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, estado) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id, 'REVOGADO' FROM papel WHERE codigo = 'ESTUDANTE'$q$,
    'chk_vinculo_papel_revogacao');
SELECT pg_temp.esperar_sucesso('vinculo_papel atribuicao revogada',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id, estado, data_revogacao) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id, 'REVOGADO', now() FROM papel WHERE codigo = 'ESTUDANTE'$q$);
SELECT pg_temp.esperar_sucesso('vinculo_papel nova atribuicao activa apos revogada',
    $q$INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT '80000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', id FROM papel WHERE codigo = 'ESTUDANTE'$q$);

SELECT pg_temp.esperar_sucesso('atribuicao_global ADMIN_GERAL activa',
    $q$INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT '70000000-0000-0000-0000-000000000002', id FROM papel WHERE codigo = 'ADMIN_GERAL'$q$);
SELECT pg_temp.esperar_falha('atribuicao_global papel INSTITUCIONAL',
    $q$INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT '70000000-0000-0000-0000-000000000002', id FROM papel WHERE codigo = 'DOCENTE'$q$,
    'fk_atribuicao_global_papel');
SELECT pg_temp.esperar_falha('atribuicao_global activa duplicada',
    $q$INSERT INTO atribuicao_global (utilizador_id, papel_id) SELECT '70000000-0000-0000-0000-000000000002', id FROM papel WHERE codigo = 'ADMIN_GERAL'$q$,
    'uq_atribuicao_global_activa');
SELECT pg_temp.esperar_falha('atribuicao_global REVOGADA sem data_revogacao',
    $q$INSERT INTO atribuicao_global (utilizador_id, papel_id, estado) SELECT '70000000-0000-0000-0000-000000000001', id, 'REVOGADA' FROM papel WHERE codigo = 'ADMIN_GERAL'$q$,
    'chk_atribuicao_global_revogacao');

SELECT pg_temp.esperar_falha('tipo_instituicao decidido_por com estado PENDENTE',
    $q$INSERT INTO tipo_instituicao (nome, decidido_por) VALUES ('Pending Decided', '70000000-0000-0000-0000-000000000002')$q$,
    'chk_tipo_instituicao_decisao');
SELECT pg_temp.esperar_falha('tipo_instituicao proposto_por inexistente',
    $q$INSERT INTO tipo_instituicao (nome, proposto_por) VALUES ('Unknown Proposer', '99999999-9999-9999-9999-999999999999')$q$,
    'tipo_instituicao_proposto_por_fkey');
SELECT pg_temp.esperar_sucesso('tipo_instituicao PENDENTE com proposto_por',
    $q$INSERT INTO tipo_instituicao (nome, proposto_por) VALUES ('Proposed Type', '70000000-0000-0000-0000-000000000001')$q$);
SELECT pg_temp.esperar_sucesso('tipo_instituicao APROVADA com proposto_por e decidido_por',
    $q$INSERT INTO tipo_instituicao (nome, estado, data_aprovacao, proposto_por, decidido_por) VALUES ('Approved Type', 'APROVADA', now(), '70000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002')$q$);

ROLLBACK;
