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

CREATE FUNCTION pg_temp.esperar_visiveis(p_teste text, p_utilizador uuid, p_esperados uuid[])
RETURNS void
LANGUAGE plpgsql
AS $f$
DECLARE
    v_obtidos uuid[];
    v_esperados uuid[];
BEGIN
    SELECT coalesce(array_agg(x ORDER BY x), '{}'::uuid[]) INTO v_obtidos
    FROM materiais_visiveis(p_utilizador) AS x;
    SELECT coalesce(array_agg(e ORDER BY e), '{}'::uuid[]) INTO v_esperados
    FROM unnest(p_esperados) AS e;
    IF v_obtidos IS DISTINCT FROM v_esperados THEN
        RAISE EXCEPTION 'TESTE FALHOU: % (esperado %, obtido %)', p_teste, v_esperados, v_obtidos;
    END IF;
    RAISE NOTICE 'OK: % -> % material(ais) visivel(eis)', p_teste, cardinality(v_obtidos);
END;
$f$;

INSERT INTO tipo_instituicao (id, nome, estado, data_aprovacao)
VALUES ('10000000-0000-0000-0000-000000000001', 'University', 'APROVADA', now());

INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Academy A', 'AA', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', 'Academy B', 'AB', '10000000-0000-0000-0000-000000000001');

INSERT INTO disciplina (id, instituicao_id, codigo, nome)
VALUES
    ('d0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'STAT1', 'Statistics'),
    ('d0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'STAT1', 'Statistics');

INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'u1@example.org', 'User 1', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000002', 'u2@example.org', 'User 2', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000003', 'u3@example.org', 'User 3', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000004', 'u4@example.org', 'User 4', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000005', 'u5@example.org', 'User 5', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000006', 'u6@example.org', 'User 6', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000007', 'u7@example.org', 'User 7', repeat('x', 60), 'SUSPENSO');

INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado, data_inicio, data_fim)
VALUES
    ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'FUNCIONARIO', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000002', 'DOCENTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'DOCENTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000005', '70000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'TERMINADO', '2020-01-01', '2021-01-01'),
    ('80000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000002', 'ESTUDANTE', 'ACTIVO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000007', '70000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'SUSPENSO', '2020-01-01', NULL),
    ('80000000-0000-0000-0000-000000000008', '70000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'ACTIVO', '2020-01-01', NULL);


SELECT pg_temp.esperar_sucesso('m1 A institucional publicado',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'INSTITUCIONAL', 'PUBLICADO', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$);

SELECT pg_temp.esperar_sucesso('m2 A biblioteca publicado',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000002', 'BIBLIOTECA', 'PUBLICADO', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$);

SELECT pg_temp.esperar_sucesso('m3 B institucional publicado',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000002', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000003', 'INSTITUCIONAL', 'PUBLICADO', '80000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000005', now())$q$);

SELECT pg_temp.esperar_sucesso('m4 B biblioteca publicado',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000002', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000004', 'BIBLIOTECA', 'PUBLICADO', '80000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000005', now())$q$);

SELECT pg_temp.esperar_sucesso('m5 A biblioteca pendente',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000005', 'BIBLIOTECA')$q$);

SELECT pg_temp.esperar_sucesso('m6 A biblioteca rejeitado com motivo',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito, estado, motivo_rejeicao, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000006', 'BIBLIOTECA', 'REJEITADO', 'Low quality', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$);

SELECT pg_temp.esperar_sucesso('m7 A biblioteca retirado',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito, estado, retirado_por, data_retirada, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000007', 'BIBLIOTECA', 'RETIRADO', '70000000-0000-0000-0000-000000000002', now(), '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$);

SELECT pg_temp.esperar_sucesso('m8 A institucional pendente',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, id, ambito) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000008', 'INSTITUCIONAL')$q$);

SELECT pg_temp.esperar_sucesso('material do tipo LIGACAO com https',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, ligacao_externa) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'LIGACAO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'https://example.org/recurso')$q$);

SELECT pg_temp.esperar_falha('material com submissor de outra instituicao',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003')$q$,
    'fk_material_submissor');

SELECT pg_temp.esperar_falha('material com utilizador que nao e o do vinculo',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002')$q$,
    'fk_material_submissor');

SELECT pg_temp.esperar_falha('material titulo vazio',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id) VALUES ('40000000-0000-0000-0000-000000000001', '   ', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001')$q$,
    'chk_material_titulo');

SELECT pg_temp.esperar_falha('material tipo invalido',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'PIRATARIA', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001')$q$,
    'chk_material_tipo');

SELECT pg_temp.esperar_falha('material ambito invalido',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, ambito) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'MUNDIAL')$q$,
    'chk_material_ambito');

SELECT pg_temp.esperar_falha('material estado invalido',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'INVALIDO')$q$,
    'chk_material_estado');

SELECT pg_temp.esperar_falha('material LIGACAO sem ligacao_externa',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'LIGACAO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001')$q$,
    'chk_material_ligacao');

SELECT pg_temp.esperar_falha('material DOCUMENTO com ligacao_externa',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, ligacao_externa) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'https://example.org/x')$q$,
    'chk_material_ligacao');

SELECT pg_temp.esperar_falha('material LIGACAO com protocolo nao http',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, ligacao_externa) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'LIGACAO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'ftp://example.org/x')$q$,
    'chk_material_ligacao');

SELECT pg_temp.esperar_falha('material PUBLICADO sem revisor',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PUBLICADO')$q$,
    'chk_material_estado_campos');

SELECT pg_temp.esperar_falha('material PENDENTE com revisor',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$,
    'chk_material_estado_campos');

SELECT pg_temp.esperar_falha('material REJEITADO sem motivo',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'REJEITADO', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$,
    'chk_material_estado_campos');

SELECT pg_temp.esperar_falha('material REJEITADO com motivo em branco',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, motivo_rejeicao, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'REJEITADO', '  ', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$,
    'chk_material_estado_campos');

SELECT pg_temp.esperar_falha('material RETIRADO sem retirado_por',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, data_retirada, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'RETIRADO', now(), '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', now())$q$,
    'chk_material_estado_campos');

SELECT pg_temp.esperar_falha('material revisor com vinculo nulo e utilizador preenchido',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PUBLICADO', '70000000-0000-0000-0000-000000000002', now())$q$,
    'chk_material_revisao_par');

SELECT pg_temp.esperar_falha('material revisto pelo proprio submissor (mesmo vinculo)',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PUBLICADO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', now())$q$,
    'chk_material_autorevisao');

SELECT pg_temp.esperar_falha('material revisto pelo proprio submissor (outro vinculo)',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PUBLICADO', '80000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000001', now())$q$,
    'chk_material_autorevisao');

SELECT pg_temp.esperar_falha('material com revisor de outra instituicao',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PUBLICADO', '80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', now())$q$,
    'fk_material_revisor');

SELECT pg_temp.esperar_falha('material com revisor cujo utilizador nao e o do vinculo',
    $q$INSERT INTO material (instituicao_id, titulo, tipo, submetido_por_vinculo_id, submetido_por_utilizador_id, estado, revisto_por_vinculo_id, revisto_por_utilizador_id, data_revisao) VALUES ('40000000-0000-0000-0000-000000000001', 'Title', 'DOCUMENTO', '80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'PUBLICADO', '80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000003', now())$q$,
    'fk_material_revisor');

SELECT pg_temp.esperar_sucesso('ficheiro_material valido',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 1, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-1')$q$);

SELECT pg_temp.esperar_sucesso('ficheiro_material segundo ficheiro do mesmo material',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 2, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-2')$q$);

SELECT pg_temp.esperar_sucesso('ficheiro_material com o mesmo hash noutro material',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000002', 1, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-3')$q$);

SELECT pg_temp.esperar_falha('ficheiro_material ordem duplicada no material',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 1, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-4')$q$,
    'uq_ficheiro_material_ordem');

SELECT pg_temp.esperar_falha('ficheiro_material chave de armazenamento duplicada',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 3, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-1')$q$,
    'uq_ficheiro_material_chave');

SELECT pg_temp.esperar_falha('ficheiro_material material de outra instituicao',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000001', 9, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-5')$q$,
    'fk_ficheiro_material_material');

SELECT pg_temp.esperar_falha('ficheiro_material hash invalido',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 4, 'notes.pdf', 'application/pdf', 1024, 'ABC', 'a/key-6')$q$,
    'chk_ficheiro_material_hash');

SELECT pg_temp.esperar_falha('ficheiro_material hash em maiusculas',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 4, 'notes.pdf', 'application/pdf', 1024, 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'a/key-7')$q$,
    'chk_ficheiro_material_hash');

SELECT pg_temp.esperar_falha('ficheiro_material tamanho zero',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 4, 'notes.pdf', 'application/pdf', 0, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-8')$q$,
    'chk_ficheiro_material_tamanho');

SELECT pg_temp.esperar_falha('ficheiro_material ordem inferior a 1',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 0, 'notes.pdf', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-9')$q$,
    'chk_ficheiro_material_ordem');

SELECT pg_temp.esperar_falha('ficheiro_material nome vazio',
    $q$INSERT INTO ficheiro_material (instituicao_id, material_id, ordem, nome_original, tipo_mime, tamanho_bytes, hash_sha256, chave_armazenamento) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 4, '  ', 'application/pdf', 1024, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'a/key-10')$q$,
    'chk_ficheiro_material_nome');

SELECT pg_temp.esperar_sucesso('material_disciplina valido',
    $q$INSERT INTO material_disciplina (instituicao_id, material_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001')$q$);

SELECT pg_temp.esperar_falha('material_disciplina duplicado',
    $q$INSERT INTO material_disciplina (instituicao_id, material_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001')$q$,
    'uq_material_disciplina');

SELECT pg_temp.esperar_falha('material_disciplina com disciplina de outra instituicao',
    $q$INSERT INTO material_disciplina (instituicao_id, material_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002')$q$,
    'fk_material_disciplina_disciplina');

SELECT pg_temp.esperar_falha('material_disciplina com material de outra instituicao',
    $q$INSERT INTO material_disciplina (instituicao_id, material_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002')$q$,
    'fk_material_disciplina_material');

SELECT pg_temp.esperar_visiveis('visibilidade u1 (activo em A)', '70000000-0000-0000-0000-000000000001', ARRAY['aa000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000004']::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade u2 revisor (activo em A)', '70000000-0000-0000-0000-000000000002', ARRAY['aa000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000004']::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade u3 (activo em B)', '70000000-0000-0000-0000-000000000003', ARRAY['aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000004']::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade u4 sem vinculos', '70000000-0000-0000-0000-000000000004', ARRAY['aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000004']::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade u5 (vinculo A terminado, B activo)', '70000000-0000-0000-0000-000000000005', ARRAY['aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-000000000004']::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade u6 (vinculo A suspenso)', '70000000-0000-0000-0000-000000000006', ARRAY['aa000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-000000000004']::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade u7 (conta suspensa)', '70000000-0000-0000-0000-000000000007', '{}'::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade visitante anonimo (NULL)', NULL::uuid, '{}'::uuid[]);

SELECT pg_temp.esperar_visiveis('visibilidade utilizador inexistente', '99999999-9999-9999-9999-999999999999', '{}'::uuid[]);

ROLLBACK;
