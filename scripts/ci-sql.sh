#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."
export PGCLIENTENCODING=UTF8
falhou=0

verificar() {
  ficheiro="$1"
  esperado="$2"
  saida=$(psql -X -v ON_ERROR_STOP=1 -f "src/test/sql/$ficheiro" 2>&1)
  codigo=$?
  ok=$(printf '%s\n' "$saida" | grep -c 'NOTICE:  OK')
  erros=$(printf '%s\n' "$saida" | grep -cE 'FALHOU|ERROR')
  if [ "$codigo" -ne 0 ] || [ "$ok" -ne "$esperado" ] || [ "$erros" -ne 0 ]; then
    echo "FALHOU  $ficheiro: codigo=$codigo ok=$ok esperado=$esperado linhas_de_erro=$erros"
    printf '%s\n' "$saida" | grep -E 'FALHOU|ERROR' | head -5
    falhou=1
  else
    echo "OK      $ficheiro: $ok verificacoes"
  fi
}

verificar T_fase1.sql 21
verificar T_fase2.sql 38
verificar T_fase3a.sql 58
verificar T_fase3b.sql 52
verificar T_autenticacao.sql 35
verificar T_contas.sql 31

exit $falhou
