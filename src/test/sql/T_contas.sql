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

INSERT INTO utilizador (id, email, nome_completo, palavra_passe_hash, estado)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'ana@example.org', 'Ana', repeat('x', 60), 'ACTIVO'),
    ('70000000-0000-0000-0000-000000000002', 'pendente@example.org', 'Pendente', repeat('x', 60), 'PENDENTE'),
    ('70000000-0000-0000-0000-000000000003', 'susp@example.org', 'Suspenso', repeat('x', 60), 'SUSPENSO');


SELECT pg_temp.esperar_falha('token com hash curto',
    $q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000001', 'VERIFICACAO_EMAIL', 'abc', now() + interval '1 day')$q$,
    'chk_token_utilizador_hash');

SELECT pg_temp.esperar_falha('token com hash em maiusculas',
    $q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000001', 'VERIFICACAO_EMAIL', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', now() + interval '1 day')$q$,
    'chk_token_utilizador_hash');

SELECT pg_temp.esperar_falha('token com tipo invalido',
    $q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000001', 'OUTRO', '1111111111111111111111111111111111111111111111111111111111111111', now() + interval '1 day')$q$,
    'chk_token_utilizador_tipo');

SELECT pg_temp.esperar_falha('token que expira antes de ser criado',
    $q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000001', 'VERIFICACAO_EMAIL', '1111111111111111111111111111111111111111111111111111111111111111', now() - interval '1 day')$q$,
    'chk_token_utilizador_validade');

SELECT pg_temp.esperar_falha('token de utilizador inexistente',
    $q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('99999999-9999-9999-9999-999999999999', 'VERIFICACAO_EMAIL', '1111111111111111111111111111111111111111111111111111111111111111', now() + interval '1 day')$q$,
    'token_utilizador_utilizador_id_fkey');

SELECT pg_temp.afirmar('procurar conta ignora maiusculas e espacos e devolve o estado', (SELECT estado FROM (SELECT id, estado, nome_completo FROM utilizador WHERE lower(btrim(email)) = lower(btrim('  PENDENTE@Example.org '))) t) = 'PENDENTE');

SELECT pg_temp.afirmar('procurar conta inexistente nao devolve linhas', NOT EXISTS (SELECT 1 FROM (SELECT id, estado, nome_completo FROM utilizador WHERE lower(btrim(email)) = lower(btrim('ninguem@example.org'))) t));

SELECT pg_temp.afirmar('criar pendente com email novo insere uma linha', pg_temp.linhas($q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES (lower(btrim('  Nova@Example.ORG ')), btrim('  Nova Pessoa '), 'zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz') ON CONFLICT DO NOTHING RETURNING id$q$) = 1);

SELECT pg_temp.afirmar('criar pendente normaliza o email, o nome e fica PENDENTE', (SELECT email || '|' || nome_completo || '|' || estado FROM utilizador WHERE email = 'nova@example.org') = 'nova@example.org|Nova Pessoa|PENDENTE');

SELECT pg_temp.afirmar('criar pendente com email ja existente nao insere nem falha', pg_temp.linhas($q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES (lower(btrim('NOVA@example.org')), btrim('Outra'), 'zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz') ON CONFLICT DO NOTHING RETURNING id$q$) = 0);

SELECT pg_temp.afirmar('criar pendente com email de conta activa nao insere nem falha', pg_temp.linhas($q$INSERT INTO utilizador (email, nome_completo, palavra_passe_hash) VALUES (lower(btrim('ana@example.org')), btrim('Outra'), 'zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz') ON CONFLICT DO NOTHING RETURNING id$q$) = 0);

SELECT pg_temp.executar($q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000002'::uuid, 'VERIFICACAO_EMAIL', '1111111111111111111111111111111111111111111111111111111111111111', now() + make_interval(secs => 86400.0))$q$);

SELECT pg_temp.afirmar('emitido recentemente: token acabado de criar', (SELECT EXISTS (SELECT 1 FROM token_utilizador WHERE utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND tipo = 'VERIFICACAO_EMAIL' AND criado_em > now() - make_interval(secs => 120.0))));

SELECT pg_temp.afirmar('emitido recentemente: outro tipo de token nao conta', NOT (SELECT EXISTS (SELECT 1 FROM token_utilizador WHERE utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND tipo = 'REDEFINICAO_PALAVRA_PASSE' AND criado_em > now() - make_interval(secs => 120.0))));

SELECT pg_temp.afirmar('emitido recentemente: outro utilizador nao conta', NOT (SELECT EXISTS (SELECT 1 FROM token_utilizador WHERE utilizador_id = '70000000-0000-0000-0000-000000000001'::uuid AND tipo = 'VERIFICACAO_EMAIL' AND criado_em > now() - make_interval(secs => 120.0))));

SELECT pg_temp.executar($q$UPDATE token_utilizador SET criado_em = now() - interval '10 minutes' WHERE hash_token = '1111111111111111111111111111111111111111111111111111111111111111'$q$);

SELECT pg_temp.afirmar('emitido recentemente: token com 10 minutos ja nao conta', NOT (SELECT EXISTS (SELECT 1 FROM token_utilizador WHERE utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND tipo = 'VERIFICACAO_EMAIL' AND criado_em > now() - make_interval(secs => 120.0))));

SELECT pg_temp.afirmar('consumir token valido devolve o utilizador', (SELECT consumir_token('1111111111111111111111111111111111111111111111111111111111111111', 'VERIFICACAO_EMAIL')) = '70000000-0000-0000-0000-000000000002'::uuid);

SELECT pg_temp.afirmar('consumir o mesmo token duas vezes: a segunda devolve NULL', (SELECT consumir_token('1111111111111111111111111111111111111111111111111111111111111111', 'VERIFICACAO_EMAIL')) IS NULL);

SELECT pg_temp.afirmar('consumir token desconhecido devolve NULL', (SELECT consumir_token('9999999999999999999999999999999999999999999999999999999999999999', 'VERIFICACAO_EMAIL')) IS NULL);

SELECT pg_temp.executar($q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000002'::uuid, 'VERIFICACAO_EMAIL', '2222222222222222222222222222222222222222222222222222222222222222', now() + make_interval(secs => 86400.0))$q$);

SELECT pg_temp.executar($q$UPDATE token_utilizador SET criado_em = now() - interval '2 days', expira_em = now() - interval '1 day' WHERE hash_token = '2222222222222222222222222222222222222222222222222222222222222222'$q$);

SELECT pg_temp.afirmar('consumir token expirado devolve NULL', (SELECT consumir_token('2222222222222222222222222222222222222222222222222222222222222222', 'VERIFICACAO_EMAIL')) IS NULL);

SELECT pg_temp.executar($q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000002'::uuid, 'VERIFICACAO_EMAIL', '3333333333333333333333333333333333333333333333333333333333333333', now() + make_interval(secs => 86400.0))$q$);

SELECT pg_temp.afirmar('consumir com o tipo errado devolve NULL', (SELECT consumir_token('3333333333333333333333333333333333333333333333333333333333333333', 'REDEFINICAO_PALAVRA_PASSE')) IS NULL);

SELECT pg_temp.afirmar('o token do tipo errado continua utilizavel com o tipo certo', (SELECT consumir_token('3333333333333333333333333333333333333333333333333333333333333333', 'VERIFICACAO_EMAIL')) = '70000000-0000-0000-0000-000000000002'::uuid);

SELECT pg_temp.executar($q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000002'::uuid, 'VERIFICACAO_EMAIL', '4444444444444444444444444444444444444444444444444444444444444444', now() + make_interval(secs => 86400.0))$q$);

SELECT pg_temp.executar($q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000001'::uuid, 'VERIFICACAO_EMAIL', '5555555555555555555555555555555555555555555555555555555555555555', now() + make_interval(secs => 86400.0))$q$);

SELECT pg_temp.afirmar('invalidar activos afecta os tokens do utilizador e do tipo ainda nao invalidados (o activo e o expirado)', pg_temp.linhas($q$UPDATE token_utilizador SET invalidado_em = now() WHERE utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND tipo = 'VERIFICACAO_EMAIL' AND invalidado_em IS NULL$q$) = 2);

SELECT pg_temp.afirmar('token invalidado nao pode ser consumido', (SELECT consumir_token('4444444444444444444444444444444444444444444444444444444444444444', 'VERIFICACAO_EMAIL')) IS NULL);

SELECT pg_temp.afirmar('invalidar tokens de um utilizador nao afecta os de outro', (SELECT consumir_token('5555555555555555555555555555555555555555555555555555555555555555', 'VERIFICACAO_EMAIL')) = '70000000-0000-0000-0000-000000000001'::uuid);

SELECT pg_temp.executar($q$INSERT INTO token_utilizador (utilizador_id, tipo, hash_token, expira_em) VALUES ('70000000-0000-0000-0000-000000000002'::uuid, 'REDEFINICAO_PALAVRA_PASSE', '6666666666666666666666666666666666666666666666666666666666666666', now() + make_interval(secs => 86400.0))$q$);

SELECT pg_temp.afirmar('invalidar tokens de verificacao nao afecta os de redefinicao', pg_temp.linhas($q$UPDATE token_utilizador SET invalidado_em = now() WHERE utilizador_id = '70000000-0000-0000-0000-000000000002'::uuid AND tipo = 'VERIFICACAO_EMAIL' AND invalidado_em IS NULL$q$) = 0);

SELECT pg_temp.afirmar('o token de redefinicao continua valido', (SELECT consumir_token('6666666666666666666666666666666666666666666666666666666666666666', 'REDEFINICAO_PALAVRA_PASSE')) = '70000000-0000-0000-0000-000000000002'::uuid);

SELECT pg_temp.afirmar('activar conta PENDENTE actualiza uma linha', pg_temp.linhas($q$UPDATE utilizador SET palavra_passe_hash = 'yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy', estado = 'ACTIVO' WHERE id = '70000000-0000-0000-0000-000000000002'::uuid AND estado = 'PENDENTE'$q$) = 1);

SELECT pg_temp.afirmar('conta activada fica ACTIVO com a nova palavra-passe', (SELECT estado = 'ACTIVO' AND palavra_passe_hash = 'yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy' FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000002'::uuid));

SELECT pg_temp.afirmar('activar uma conta ja activa nao faz nada', pg_temp.linhas($q$UPDATE utilizador SET palavra_passe_hash = 'wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww', estado = 'ACTIVO' WHERE id = '70000000-0000-0000-0000-000000000002'::uuid AND estado = 'PENDENTE'$q$) = 0);

SELECT pg_temp.afirmar('activar uma conta SUSPENSO nao faz nada', pg_temp.linhas($q$UPDATE utilizador SET palavra_passe_hash = 'wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww', estado = 'ACTIVO' WHERE id = '70000000-0000-0000-0000-000000000003'::uuid AND estado = 'PENDENTE'$q$) = 0);

SELECT pg_temp.afirmar('a conta SUSPENSO continua suspensa e com a palavra-passe antiga', (SELECT estado = 'SUSPENSO' AND palavra_passe_hash = repeat('x', 60) FROM utilizador WHERE id = '70000000-0000-0000-0000-000000000003'::uuid));

ROLLBACK;
