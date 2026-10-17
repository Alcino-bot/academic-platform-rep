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
    ('70000000-0000-0000-0000-000000000001', 'student@example.org', 'Student Test', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000002', 'teacher@example.org', 'Teacher Test', repeat('x', 60), 'ACTIVO');

INSERT INTO vinculo_institucional (id, utilizador_id, instituicao_id, tipo_vinculo, estado)
VALUES
    ('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'ESTUDANTE', 'ACTIVO'),
    ('80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'DOCENTE', 'ACTIVO'),
    ('80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'DOCENTE', 'ACTIVO');


SELECT pg_temp.esperar_sucesso('ano_lectivo instituicao A',
    $q$INSERT INTO ano_lectivo (id, instituicao_id, nome, data_inicio, data_fim) VALUES ('a0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '2026', '2026-02-01', '2026-11-30')$q$);

SELECT pg_temp.esperar_sucesso('ano_lectivo mesmo nome noutra instituicao',
    $q$INSERT INTO ano_lectivo (id, instituicao_id, nome, data_inicio, data_fim) VALUES ('a0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '2026', '2026-02-01', '2026-11-30')$q$);

SELECT pg_temp.esperar_falha('ano_lectivo nome duplicado normalizado',
    $q$INSERT INTO ano_lectivo (instituicao_id, nome, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', ' 2026 ', '2027-02-01', '2027-11-30')$q$,
    'uq_ano_lectivo_nome');

SELECT pg_temp.esperar_falha('ano_lectivo data_fim nao posterior a data_inicio',
    $q$INSERT INTO ano_lectivo (instituicao_id, nome, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', '2027', '2027-02-01', '2027-02-01')$q$,
    'chk_ano_lectivo_datas');

SELECT pg_temp.esperar_falha('ano_lectivo estado invalido',
    $q$INSERT INTO ano_lectivo (instituicao_id, nome, data_inicio, data_fim, estado) VALUES ('40000000-0000-0000-0000-000000000001', '2027', '2027-02-01', '2027-11-30', 'INVALIDO')$q$,
    'chk_ano_lectivo_estado');

SELECT pg_temp.esperar_falha('ano_lectivo nome vazio',
    $q$INSERT INTO ano_lectivo (instituicao_id, nome, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', '  ', '2027-02-01', '2027-11-30')$q$,
    'chk_ano_lectivo_nome');

SELECT pg_temp.esperar_sucesso('periodo 1 da instituicao A',
    $q$INSERT INTO periodo (id, instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('b0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Semester 1', 1, '2026-02-01', '2026-06-30')$q$);

SELECT pg_temp.esperar_sucesso('periodo 2 da instituicao A',
    $q$INSERT INTO periodo (instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Semester 2', 2, '2026-07-15', '2026-11-30')$q$);

SELECT pg_temp.esperar_sucesso('periodo da instituicao B',
    $q$INSERT INTO periodo (id, instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('b0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Trimester 1', 1, '2026-02-01', '2026-05-30')$q$);

SELECT pg_temp.esperar_falha('periodo com ano lectivo de outra instituicao',
    $q$INSERT INTO periodo (instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Cross', 3, '2026-02-01', '2026-03-01')$q$,
    'fk_periodo_ano_lectivo');

SELECT pg_temp.esperar_falha('periodo ordem duplicada no mesmo ano',
    $q$INSERT INTO periodo (instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Another', 1, '2026-02-01', '2026-03-01')$q$,
    'uq_periodo_ordem');

SELECT pg_temp.esperar_falha('periodo nome duplicado normalizado no mesmo ano',
    $q$INSERT INTO periodo (instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', ' semester 1 ', 3, '2026-02-01', '2026-03-01')$q$,
    'uq_periodo_nome');

SELECT pg_temp.esperar_falha('periodo ordem inferior a 1',
    $q$INSERT INTO periodo (instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Zero', 0, '2026-02-01', '2026-03-01')$q$,
    'chk_periodo_ordem');

SELECT pg_temp.esperar_falha('periodo data_fim anterior a data_inicio',
    $q$INSERT INTO periodo (instituicao_id, ano_lectivo_id, nome, ordem, data_inicio, data_fim) VALUES ('40000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Backwards', 4, '2026-03-01', '2026-02-01')$q$,
    'chk_periodo_datas');

SELECT pg_temp.esperar_sucesso('curso instituicao A',
    $q$INSERT INTO curso (id, instituicao_id, unidade_id, codigo, nome) VALUES ('c0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'CS', 'Computer Science')$q$);

SELECT pg_temp.esperar_sucesso('curso instituicao B',
    $q$INSERT INTO curso (id, instituicao_id, unidade_id, codigo, nome) VALUES ('c0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', 'CS', 'Computer Science')$q$);

SELECT pg_temp.esperar_sucesso('dois cursos sem codigo na mesma unidade',
    $q$INSERT INTO curso (instituicao_id, unidade_id, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'Course No Code 1'), ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'Course No Code 2')$q$);

SELECT pg_temp.esperar_falha('curso com unidade de outra instituicao',
    $q$INSERT INTO curso (instituicao_id, unidade_id, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', 'Cross Unit')$q$,
    'fk_curso_unidade');

SELECT pg_temp.esperar_falha('curso nome duplicado normalizado na mesma unidade',
    $q$INSERT INTO curso (instituicao_id, unidade_id, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '  computer science ')$q$,
    'uq_curso_nome');

SELECT pg_temp.esperar_falha('curso codigo duplicado normalizado na instituicao',
    $q$INSERT INTO curso (instituicao_id, unidade_id, codigo, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', ' cs ', 'Other Name')$q$,
    'uq_curso_codigo');

SELECT pg_temp.esperar_falha('curso estado invalido',
    $q$INSERT INTO curso (instituicao_id, unidade_id, nome, estado) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'Bad State', 'INVALIDO')$q$,
    'chk_curso_estado');

SELECT pg_temp.esperar_falha('curso codigo so com espacos',
    $q$INSERT INTO curso (instituicao_id, unidade_id, codigo, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '   ', 'Blank Code')$q$,
    'chk_curso_codigo');

SELECT pg_temp.esperar_sucesso('disciplina instituicao A sem unidade',
    $q$INSERT INTO disciplina (id, instituicao_id, codigo, nome, carga_horaria) VALUES ('d0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'STAT1', 'Statistics', 60)$q$);

SELECT pg_temp.esperar_sucesso('disciplina instituicao B',
    $q$INSERT INTO disciplina (id, instituicao_id, unidade_id, codigo, nome) VALUES ('d0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', 'STAT1', 'Statistics')$q$);

SELECT pg_temp.esperar_sucesso('disciplina com unidade responsavel da propria instituicao',
    $q$INSERT INTO disciplina (instituicao_id, unidade_id, codigo, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'PROG1', 'Programming')$q$);

SELECT pg_temp.esperar_falha('disciplina codigo duplicado normalizado',
    $q$INSERT INTO disciplina (instituicao_id, codigo, nome) VALUES ('40000000-0000-0000-0000-000000000001', ' stat1 ', 'Other')$q$,
    'uq_disciplina_codigo');

SELECT pg_temp.esperar_falha('disciplina com unidade de outra instituicao',
    $q$INSERT INTO disciplina (instituicao_id, unidade_id, codigo, nome) VALUES ('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', 'X1', 'Cross')$q$,
    'fk_disciplina_unidade');

SELECT pg_temp.esperar_falha('disciplina carga horaria nao positiva',
    $q$INSERT INTO disciplina (instituicao_id, codigo, nome, carga_horaria) VALUES ('40000000-0000-0000-0000-000000000001', 'X2', 'Zero Hours', 0)$q$,
    'chk_disciplina_carga_horaria');

SELECT pg_temp.esperar_sucesso('curso_disciplina disciplina no curso CS',
    $q$INSERT INTO curso_disciplina (instituicao_id, curso_id, disciplina_id, ano_curricular, periodo_curricular) VALUES ('40000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 1, 2)$q$);

SELECT pg_temp.esperar_sucesso('disciplina partilhada por um segundo curso',
    $q$INSERT INTO curso_disciplina (instituicao_id, curso_id, disciplina_id) SELECT '40000000-0000-0000-0000-000000000001', c.id, 'd0000000-0000-0000-0000-000000000001' FROM curso c WHERE c.nome = 'Course No Code 1'$q$);

SELECT pg_temp.esperar_falha('curso_disciplina duplicado',
    $q$INSERT INTO curso_disciplina (instituicao_id, curso_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001')$q$,
    'uq_curso_disciplina');

SELECT pg_temp.esperar_falha('curso_disciplina com curso de outra instituicao',
    $q$INSERT INTO curso_disciplina (instituicao_id, curso_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001')$q$,
    'fk_curso_disciplina_curso');

SELECT pg_temp.esperar_falha('curso_disciplina com disciplina de outra instituicao',
    $q$INSERT INTO curso_disciplina (instituicao_id, curso_id, disciplina_id) VALUES ('40000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002')$q$,
    'fk_curso_disciplina_disciplina');

SELECT pg_temp.esperar_falha('curso_disciplina ano curricular inferior a 1',
    $q$INSERT INTO curso_disciplina (instituicao_id, curso_id, disciplina_id, ano_curricular) SELECT '40000000-0000-0000-0000-000000000001', c.id, d.id, 0 FROM curso c, disciplina d WHERE c.nome = 'Course No Code 2' AND d.codigo = 'PROG1'$q$,
    'chk_curso_disciplina_ano');

SELECT pg_temp.esperar_sucesso('turma instituicao A',
    $q$INSERT INTO turma (id, instituicao_id, disciplina_id, periodo_id, codigo, capacidade) VALUES ('e0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'A', 40)$q$);

SELECT pg_temp.esperar_sucesso('turma instituicao B',
    $q$INSERT INTO turma (id, instituicao_id, disciplina_id, periodo_id, codigo) VALUES ('e0000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'A')$q$);

SELECT pg_temp.esperar_falha('turma com disciplina de outra instituicao',
    $q$INSERT INTO turma (instituicao_id, disciplina_id, periodo_id, codigo) VALUES ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'Z')$q$,
    'fk_turma_disciplina');

SELECT pg_temp.esperar_falha('turma com periodo de outra instituicao',
    $q$INSERT INTO turma (instituicao_id, disciplina_id, periodo_id, codigo) VALUES ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'Z')$q$,
    'fk_turma_periodo');

SELECT pg_temp.esperar_falha('turma codigo duplicado na disciplina e periodo',
    $q$INSERT INTO turma (instituicao_id, disciplina_id, periodo_id, codigo) VALUES ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', ' a ')$q$,
    'uq_turma_codigo');

SELECT pg_temp.esperar_falha('turma capacidade nao positiva',
    $q$INSERT INTO turma (instituicao_id, disciplina_id, periodo_id, codigo, capacidade) VALUES ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'C', 0)$q$,
    'chk_turma_capacidade');

SELECT pg_temp.esperar_falha('turma estado invalido',
    $q$INSERT INTO turma (instituicao_id, disciplina_id, periodo_id, codigo, estado) VALUES ('40000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'D', 'INVALIDO')$q$,
    'chk_turma_estado');

SELECT pg_temp.esperar_sucesso('docente_turma com vinculo DOCENTE',
    $q$INSERT INTO docente_turma (instituicao_id, turma_id, vinculo_id) VALUES ('40000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000002')$q$);

SELECT pg_temp.esperar_falha('docente_turma com vinculo de ESTUDANTE',
    $q$INSERT INTO docente_turma (instituicao_id, turma_id, vinculo_id) VALUES ('40000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001')$q$,
    'fk_docente_turma_vinculo');

SELECT pg_temp.esperar_falha('docente_turma com vinculo de outra instituicao',
    $q$INSERT INTO docente_turma (instituicao_id, turma_id, vinculo_id) VALUES ('40000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000003')$q$,
    'fk_docente_turma_vinculo');

SELECT pg_temp.esperar_falha('docente_turma com turma de outra instituicao',
    $q$INSERT INTO docente_turma (instituicao_id, turma_id, vinculo_id) VALUES ('40000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000002')$q$,
    'fk_docente_turma_turma');

SELECT pg_temp.esperar_falha('docente_turma duplicado',
    $q$INSERT INTO docente_turma (instituicao_id, turma_id, vinculo_id) VALUES ('40000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000002')$q$,
    'uq_docente_turma');

SELECT pg_temp.esperar_falha('docente_turma funcao invalida',
    $q$INSERT INTO docente_turma (instituicao_id, turma_id, vinculo_id, funcao) VALUES ('40000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000002', 'CHEFE')$q$,
    'chk_docente_turma_funcao');

SELECT pg_temp.esperar_sucesso('matricula de estudante',
    $q$INSERT INTO matricula (id, instituicao_id, vinculo_id, curso_id, ano_lectivo_id) VALUES ('f0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001')$q$);

SELECT pg_temp.esperar_falha('matricula com vinculo de DOCENTE',
    $q$INSERT INTO matricula (instituicao_id, vinculo_id, curso_id, ano_lectivo_id) VALUES ('40000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001')$q$,
    'fk_matricula_vinculo');

SELECT pg_temp.esperar_falha('matricula com curso de outra instituicao',
    $q$INSERT INTO matricula (instituicao_id, vinculo_id, curso_id, ano_lectivo_id) VALUES ('40000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001')$q$,
    'fk_matricula_curso');

SELECT pg_temp.esperar_falha('matricula com ano lectivo de outra instituicao',
    $q$INSERT INTO matricula (instituicao_id, vinculo_id, curso_id, ano_lectivo_id) VALUES ('40000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002')$q$,
    'fk_matricula_ano_lectivo');

SELECT pg_temp.esperar_falha('matricula duplicada no mesmo curso e ano',
    $q$INSERT INTO matricula (instituicao_id, vinculo_id, curso_id, ano_lectivo_id) VALUES ('40000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001')$q$,
    'uq_matricula');

SELECT pg_temp.esperar_falha('matricula estado invalido',
    $q$INSERT INTO matricula (instituicao_id, vinculo_id, curso_id, ano_lectivo_id, estado) SELECT '40000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', c.id, 'a0000000-0000-0000-0000-000000000001', 'INVALIDO' FROM curso c WHERE c.nome = 'Course No Code 1'$q$,
    'chk_matricula_estado');

SELECT pg_temp.esperar_sucesso('inscricao em turma',
    $q$INSERT INTO inscricao (instituicao_id, matricula_id, turma_id) VALUES ('40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001')$q$);

SELECT pg_temp.esperar_falha('inscricao com turma de outra instituicao',
    $q$INSERT INTO inscricao (instituicao_id, matricula_id, turma_id) VALUES ('40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002')$q$,
    'fk_inscricao_turma');

SELECT pg_temp.esperar_falha('inscricao com instituicao diferente da matricula',
    $q$INSERT INTO inscricao (instituicao_id, matricula_id, turma_id) VALUES ('40000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002')$q$,
    'fk_inscricao_matricula');

SELECT pg_temp.esperar_falha('inscricao duplicada',
    $q$INSERT INTO inscricao (instituicao_id, matricula_id, turma_id) VALUES ('40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001')$q$,
    'uq_inscricao');

SELECT pg_temp.esperar_falha('inscricao estado invalido',
    $q$INSERT INTO inscricao (instituicao_id, matricula_id, turma_id, estado) SELECT '40000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', t.id, 'INVALIDO' FROM turma t WHERE t.id = 'e0000000-0000-0000-0000-000000000001'$q$,
    'chk_inscricao_estado');

ROLLBACK;
