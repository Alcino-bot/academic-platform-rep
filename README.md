# Plataforma Académica Digital

Sistema de informação web, multi-institucional e configurável, para comunicação académica, trabalhos, disciplinas e uma biblioteca colaborativa moderada. Projecto em construção: veja `docs/ESTADO.md` para saber o que já existe.

## Requisitos

- JDK 21
- Maven 3.9 ou superior
- PostgreSQL 18 (a base usa `uuidv7()`, que só existe a partir da versão 18)
- Git

## Bases de dados

São necessárias duas bases no mesmo servidor PostgreSQL:

- `plataforma_academica_dev`: para correr a aplicação.
- `plataforma_academica_teste`: para os testes. O Flyway cria as tabelas na primeira corrida.

```powershell
psql -h localhost -U postgres -d postgres -c "CREATE DATABASE plataforma_academica_dev"
psql -h localhost -U postgres -d postgres -c "CREATE DATABASE plataforma_academica_teste"
```

## Configuração

A palavra-passe da base nunca fica em ficheiros. Define-a no terminal onde vais trabalhar:

```powershell
$s = Read-Host 'Palavra-passe do postgres' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $s).Password
$env:PGPASSWORD = $env:DB_PASSWORD
$env:PGCLIENTENCODING = 'UTF8'
```

Variáveis opcionais: `DB_NAME` (por omissão `plataforma_academica_dev`), `DB_USER` (por omissão `postgres`) e `DB_NAME_TESTE`.

## Arrancar a aplicação

```powershell
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx512m -XX:TieredStopAtLevel=1"
```

O Flyway aplica as migrações em `src/main/resources/db/migration` ao arrancar. Num computador fraco o arranque pode levar alguns minutos. Verifica com `GET http://localhost:8080/api/estado`.

Em desenvolvimento, o email de verificação não é enviado: a ligação de activação aparece no registo da aplicação.

## Testes

Testes Java (58). O Flyway recria a base de teste a partir das migrações:

```powershell
mvn test "-DargLine=-Xmx512m"
```

Testes SQL (235 verificações em 6 ficheiros), contra a base de teste já migrada:

```powershell
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psql -X -v ON_ERROR_STOP=1 -h localhost -U postgres -d plataforma_academica_teste -f "src\test\sql\T_contas.sql"
```

Em Linux ou macOS, `bash scripts/ci-sql.sh` corre os seis ficheiros e confere o número esperado de verificações. É o que a integração contínua usa.

Verificação com a aplicação real (arranca a aplicação, percorre o fluxo de contas e sessão e escreve um relatório):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verificar-fluxo.ps1
```

## Estrutura

- `src/main/java`: código da aplicação (pacote `mz.plataformaacademica`).
- `src/main/resources/db/migration`: migrações Flyway V1 a V20. **Nunca editar uma migração já commitada**: corrige-se com uma migração nova.
- `src/test/java` e `src/test/sql`: testes.
- `scripts`: verificação do fluxo real e execução dos testes SQL.
- `docs`: estado, decisões, backlog e regras pendentes.

## Documentação

- `docs/ESTADO.md`: ler primeiro.
- `docs/decisoes.md`: decisões de arquitectura.
- `docs/BACKLOG.md`: trabalho por fazer.
- `docs/REGRAS_PENDENTES.md`: regras que ficam para o Java.

## Regras de trabalho

- Ficheiros em UTF-8 sem BOM.
- O identificador da instituição vem sempre da sessão, nunca do pedido.
- Toda a pesquisa de materiais passa pela função `materiais_visiveis`.
- Cada consulta nova com dados de instituições leva um teste que prova o isolamento.
