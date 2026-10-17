BEGIN;

INSERT INTO tipo_instituicao (id, nome)
VALUES ('10000000-0000-0000-0000-000000000001', 'University');

INSERT INTO tipo_instituicao (nome) VALUES ('Normalized Type');
DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO tipo_instituicao (nome) VALUES ('  NORMALIZED TYPE  ');
    RAISE EXCEPTION 'TESTE FALHOU: V1 nome duplicado normalizado';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_tipo_instituicao_nome' THEN
        RAISE EXCEPTION 'TESTE FALHOU: V1 nome duplicado normalizado (esperado %, obtido %)', 'uq_tipo_instituicao_nome', v_constraint;
    END IF;
    RAISE NOTICE 'OK: V1 nome duplicado normalizado -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO tipo_instituicao (nome, estado) VALUES ('Approved Without Date', 'APROVADA');
    RAISE EXCEPTION 'TESTE FALHOU: V1 APROVADA sem data_aprovacao';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_tipo_instituicao_aprovacao' THEN
        RAISE EXCEPTION 'TESTE FALHOU: V1 APROVADA sem data_aprovacao (esperado %, obtido %)', 'chk_tipo_instituicao_aprovacao', v_constraint;
    END IF;
    RAISE NOTICE 'OK: V1 APROVADA sem data_aprovacao -> %', v_constraint;
END;
$test$;

INSERT INTO modelo_organizacional (id, tipo_instituicao_id, nome)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Model A'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Model B');

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_organizacional (tipo_instituicao_id, nome)
    VALUES ('10000000-0000-0000-0000-000000000001', '  model a  ');
    RAISE EXCEPTION 'TESTE FALHOU: modelo_organizacional nome duplicado normalizado';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_modelo_organizacional_nome' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_organizacional nome duplicado normalizado (esperado %, obtido %)', 'uq_modelo_organizacional_nome', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_organizacional nome duplicado normalizado -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_organizacional (tipo_instituicao_id, nome, estado)
    VALUES ('10000000-0000-0000-0000-000000000001', 'Invalid Model State', 'INVALIDO');
    RAISE EXCEPTION 'TESTE FALHOU: modelo_organizacional estado invalido';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_modelo_organizacional_estado' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_organizacional estado invalido (esperado %, obtido %)', 'chk_modelo_organizacional_estado', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_organizacional estado invalido -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_organizacional (tipo_instituicao_id, nome)
    VALUES ('10000000-0000-0000-0000-000000000001', '');
    RAISE EXCEPTION 'TESTE FALHOU: modelo_organizacional nome vazio';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_modelo_organizacional_nome' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_organizacional nome vazio (esperado %, obtido %)', 'chk_modelo_organizacional_nome', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_organizacional nome vazio -> %', v_constraint;
END;
$test$;

INSERT INTO modelo_tipo_unidade (id, modelo_id, nome, nivel)
VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Model A Root', 1),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Model B Root', 1);

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_tipo_unidade (modelo_id, nome, nivel, tipo_pai_id)
    VALUES ('20000000-0000-0000-0000-000000000001', 'Level One With Parent', 1, '30000000-0000-0000-0000-000000000001');
    RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade nivel 1 com pai';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_modelo_tipo_unidade_pai' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade nivel 1 com pai (esperado %, obtido %)', 'chk_modelo_tipo_unidade_pai', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_tipo_unidade nivel 1 com pai -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_tipo_unidade (modelo_id, nome, nivel)
    VALUES ('20000000-0000-0000-0000-000000000001', 'Level Two Without Parent', 2);
    RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade nivel 2 sem pai';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_modelo_tipo_unidade_pai' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade nivel 2 sem pai (esperado %, obtido %)', 'chk_modelo_tipo_unidade_pai', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_tipo_unidade nivel 2 sem pai -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_tipo_unidade (id, modelo_id, nome, nivel, tipo_pai_id)
    VALUES ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'Self Parent Type', 2, '30000000-0000-0000-0000-000000000003');
    RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade pai de si proprio';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_modelo_tipo_unidade_pai_distinto' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade pai de si proprio (esperado %, obtido %)', 'chk_modelo_tipo_unidade_pai_distinto', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_tipo_unidade pai de si proprio -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO modelo_tipo_unidade (modelo_id, nome, nivel, tipo_pai_id)
    VALUES ('20000000-0000-0000-0000-000000000002', 'Parent From Other Model', 2, '30000000-0000-0000-0000-000000000001');
    RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade pai pertence a outro modelo';
EXCEPTION WHEN foreign_key_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'fk_modelo_tipo_unidade_pai' THEN
        RAISE EXCEPTION 'TESTE FALHOU: modelo_tipo_unidade pai pertence a outro modelo (esperado %, obtido %)', 'fk_modelo_tipo_unidade_pai', v_constraint;
    END IF;
    RAISE NOTICE 'OK: modelo_tipo_unidade pai pertence a outro modelo -> %', v_constraint;
END;
$test$;

INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Academy A', 'AA', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', 'Academy B', 'AB', '10000000-0000-0000-0000-000000000001');

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO instituicao (nome, sigla, tipo_instituicao_id)
    VALUES ('  academy a  ', 'AC', '10000000-0000-0000-0000-000000000001');
    RAISE EXCEPTION 'TESTE FALHOU: instituicao nome duplicado normalizado';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_instituicao_nome' THEN
        RAISE EXCEPTION 'TESTE FALHOU: instituicao nome duplicado normalizado (esperado %, obtido %)', 'uq_instituicao_nome', v_constraint;
    END IF;
    RAISE NOTICE 'OK: instituicao nome duplicado normalizado -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO instituicao (nome, sigla, tipo_instituicao_id)
    VALUES ('Academy C', ' aa ', '10000000-0000-0000-0000-000000000001');
    RAISE EXCEPTION 'TESTE FALHOU: instituicao sigla duplicada normalizada';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_instituicao_sigla' THEN
        RAISE EXCEPTION 'TESTE FALHOU: instituicao sigla duplicada normalizada (esperado %, obtido %)', 'uq_instituicao_sigla', v_constraint;
    END IF;
    RAISE NOTICE 'OK: instituicao sigla duplicada normalizada -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO instituicao (nome, sigla, tipo_instituicao_id, estado)
    VALUES ('Invalid Institution State', 'AIS', '10000000-0000-0000-0000-000000000001', 'INVALIDO');
    RAISE EXCEPTION 'TESTE FALHOU: instituicao estado invalido';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_instituicao_estado' THEN
        RAISE EXCEPTION 'TESTE FALHOU: instituicao estado invalido (esperado %, obtido %)', 'chk_instituicao_estado', v_constraint;
    END IF;
    RAISE NOTICE 'OK: instituicao estado invalido -> %', v_constraint;
END;
$test$;

INSERT INTO tipo_unidade (id, instituicao_id, nome, nivel)
VALUES
    ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Faculty', 1),
    ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'Other Faculty', 1);

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO tipo_unidade (instituicao_id, nome, nivel)
    VALUES ('40000000-0000-0000-0000-000000000001', 'Department', 1);
    INSERT INTO tipo_unidade (instituicao_id, nome, nivel)
    VALUES ('40000000-0000-0000-0000-000000000001', ' department ', 1);
    RAISE EXCEPTION 'TESTE FALHOU: tipo_unidade nome duplicado na mesma instituicao';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_tipo_unidade_nome' THEN
        RAISE EXCEPTION 'TESTE FALHOU: tipo_unidade nome duplicado na mesma instituicao (esperado %, obtido %)', 'uq_tipo_unidade_nome', v_constraint;
    END IF;
    RAISE NOTICE 'OK: tipo_unidade nome duplicado na mesma instituicao -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO tipo_unidade (instituicao_id, nome, nivel, tipo_pai_id)
    VALUES ('40000000-0000-0000-0000-000000000001', 'Parent From Other Institution', 2, '50000000-0000-0000-0000-000000000002');
    RAISE EXCEPTION 'TESTE FALHOU: tipo_unidade pai pertence a outra instituicao';
EXCEPTION WHEN foreign_key_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'fk_tipo_unidade_pai' THEN
        RAISE EXCEPTION 'TESTE FALHOU: tipo_unidade pai pertence a outra instituicao (esperado %, obtido %)', 'fk_tipo_unidade_pai', v_constraint;
    END IF;
    RAISE NOTICE 'OK: tipo_unidade pai pertence a outra instituicao -> %', v_constraint;
END;
$test$;

INSERT INTO unidade_organizacional (id, instituicao_id, tipo_unidade_id, nome)
VALUES
    ('60000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Parent A'),
    ('60000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', 'Parent B');

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, pai_id, nome)
    VALUES ('40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', 'Cross Institution Parent');
    RAISE EXCEPTION 'TESTE FALHOU: unidade pai pertence a outra instituicao';
EXCEPTION WHEN foreign_key_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'fk_unidade_organizacional_pai' THEN
        RAISE EXCEPTION 'TESTE FALHOU: unidade pai pertence a outra instituicao (esperado %, obtido %)', 'fk_unidade_organizacional_pai', v_constraint;
    END IF;
    RAISE NOTICE 'OK: unidade pai pertence a outra instituicao -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, nome)
    VALUES ('40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002', 'Cross Institution Type');
    RAISE EXCEPTION 'TESTE FALHOU: unidade tem tipo de outra instituicao';
EXCEPTION WHEN foreign_key_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'fk_unidade_organizacional_tipo' THEN
        RAISE EXCEPTION 'TESTE FALHOU: unidade tem tipo de outra instituicao (esperado %, obtido %)', 'fk_unidade_organizacional_tipo', v_constraint;
    END IF;
    RAISE NOTICE 'OK: unidade tem tipo de outra instituicao -> %', v_constraint;
END;
$test$;

DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO unidade_organizacional (id, instituicao_id, tipo_unidade_id, pai_id, nome)
    VALUES ('60000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000003', 'Self Parent Unit');
    RAISE EXCEPTION 'TESTE FALHOU: unidade pai de si propria';
EXCEPTION WHEN check_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'chk_unidade_organizacional_pai_distinto' THEN
        RAISE EXCEPTION 'TESTE FALHOU: unidade pai de si propria (esperado %, obtido %)', 'chk_unidade_organizacional_pai_distinto', v_constraint;
    END IF;
    RAISE NOTICE 'OK: unidade pai de si propria -> %', v_constraint;
END;
$test$;

INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, pai_id, nome)
VALUES ('40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'Same Leaf');
DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, pai_id, nome)
    VALUES ('40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', ' same leaf ');
    RAISE EXCEPTION 'TESTE FALHOU: unidades irmas com nome duplicado normalizado';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_unidade_organizacional_nome_irmas' THEN
        RAISE EXCEPTION 'TESTE FALHOU: unidades irmas com nome duplicado normalizado (esperado %, obtido %)', 'uq_unidade_organizacional_nome_irmas', v_constraint;
    END IF;
    RAISE NOTICE 'OK: unidades irmas com nome duplicado normalizado -> %', v_constraint;
END;
$test$;

INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, nome)
VALUES ('40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Same Root');
DO $test$
DECLARE
    v_constraint text;
BEGIN
    INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, nome)
    VALUES ('40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', ' same root ');
    RAISE EXCEPTION 'TESTE FALHOU: unidades raiz com nome duplicado normalizado';
EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME;
    IF v_constraint IS DISTINCT FROM 'uq_unidade_organizacional_nome_irmas' THEN
        RAISE EXCEPTION 'TESTE FALHOU: unidades raiz com nome duplicado normalizado (esperado %, obtido %)', 'uq_unidade_organizacional_nome_irmas', v_constraint;
    END IF;
    RAISE NOTICE 'OK: unidades raiz com nome duplicado normalizado -> %', v_constraint;
END;
$test$;

INSERT INTO modelo_organizacional (id, tipo_instituicao_id, nome)
VALUES ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Happy Model');
INSERT INTO modelo_tipo_unidade (id, modelo_id, nome, nivel, tipo_pai_id)
VALUES
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'Campus', 1, NULL),
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003', 'Faculty', 2, '30000000-0000-0000-0000-000000000003'),
    ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000003', 'Department', 3, '30000000-0000-0000-0000-000000000004');
INSERT INTO instituicao (id, nome, sigla, tipo_instituicao_id, modelo_origem_id)
VALUES ('40000000-0000-0000-0000-000000000003', 'Happy Academy', 'HA', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003');
INSERT INTO tipo_unidade (id, instituicao_id, nome, nivel, tipo_pai_id, modelo_tipo_origem_id)
VALUES
    ('50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003', 'Campus', 1, NULL, '30000000-0000-0000-0000-000000000003'),
    ('50000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000003', 'Faculty', 2, '50000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000004'),
    ('50000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000003', 'Department', 3, '50000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000005');
INSERT INTO unidade_organizacional (id, instituicao_id, tipo_unidade_id, pai_id, nome)
VALUES
    ('60000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000003', NULL, 'Main Campus'),
    ('60000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000010', 'Central Faculty'),
    ('60000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000011', 'Science Department');
DO $test$ BEGIN RAISE NOTICE 'OK: caso feliz modelo e arvore de 3 unidades -> criado'; END; $test$;
INSERT INTO unidade_organizacional (instituicao_id, tipo_unidade_id, pai_id, nome)
VALUES
    ('40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000011', 'Repeated Name'),
    ('40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000012', ' repeated name ');
DO $test$ BEGIN RAISE NOTICE 'OK: mesmo nome em unidades de pais diferentes -> aceite'; END; $test$;

ROLLBACK;