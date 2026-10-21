# Contexto para assistentes: Plataforma Académica Digital

Versão de 20 de Outubro de 2026, depois da fatia «Administração». Permite que uma IA (ou uma pessoa) continue o projecto sem ter assistido às conversas anteriores. Lê-o por inteiro antes de escrever código. Para o estado e o trabalho por fazer, lê também `docs/ESTADO.md`, `docs/BACKLOG.md`, `docs/decisoes.md` e `docs/REGRAS_PENDENTES.md`. Se algum destes contradisser este ficheiro, o que está em `docs/` é mais recente.

## 0. Como usar

1. Abre uma conversa nova e anexa: este ficheiro, o repositório sem histórico e, se quiseres, os quatro de `docs/` (já vão dentro do repositório). Para gerar o repositório, na pasta do projecto:
   `git checkout main` e `git pull`, depois `git archive --format=zip -o "$env:USERPROFILE\Downloads\contexto-repo.zip" HEAD`.
2. Cola o pedido inicial da secção 12.
3. No fim de cada sessão, a IA entrega as actualizações a `docs/ESTADO.md`, `docs/BACKLOG.md` e `docs/decisoes.md` **já dentro do zip da fatia**. Não há passo manual de copiar texto.

Nunca anexes palavras-passe, ficheiros `.env` nem capturas com segredos.

## 1. O projecto e o objectivo final

**Plataforma Académica Digital:** sistema de informação web (computador e telemóvel), multi-institucional e configurável, para instituições de ensino em Moçambique (ensino secundário/médio, institutos e universidades). Reúne comunicação académica, trabalhos, disciplinas e recursos de aprendizagem, e uma **biblioteca colaborativa moderada**: qualquer utilizador com vínculo activo submete materiais, mas a publicação depende de revisão e aprovação.

Decisões arquitectónicas que não se negoceiam:

- Núcleo comum com **isolamento lógico por instituição**, garantido pela base de dados e não só pelo código.
- Configuração institucional **por dados** (os modelos organizacionais são pontos de partida, copiados para cada instituição; nada de cópias da aplicação).
- Biblioteca com estado pendente e **aprovação obrigatória** antes da publicação.
- **IA** como assistente de pesquisa e operações (metadados, duplicados, pesquisa semântica, perguntas em linguagem natural). Não substitui regras determinísticas, revisão humana, autorização nem validação pedagógica. A pesquisa nunca revela conteúdo que o utilizador não pode consultar, e a permissão filtra **antes** da recuperação.

**Destino final:** plataforma utilizável por várias instituições, com piloto numa instituição real, vida académica completa (anos lectivos, cursos, disciplinas, turmas, matrículas, trabalhos), comunicação, auditoria e IA, em produção com HTTPS, cópias de segurança e monitorização. Fora do âmbito (proposto, por confirmar): notas e pautas oficiais, pagamentos, videoconferência, aplicações nativas, integração com sistemas académicos existentes.

## 2. O utilizador e o contexto de trabalho

- Estudante universitário de Ciências da Computação (2.º ano), em Moçambique. Projecto universitário. **Trabalha sozinho** (confirmado em Outubro de 2026), logo a integração contínua é o único revisor.
- Idioma: **português** (grafia do projecto: «activo», «activa», «actualizar», sem acordo ortográfico). Identificadores de código e de base de dados em português, sem acentos.
- Preferências: análise **rigorosa e não validadora** (causa raiz, crítica directa, sem lisonja); em código, exemplos concretos, executáveis, narração passo a passo e **sem comentários no código**; respostas curtas e directas (usa o telemóvel); decisões em **opções curtas**, com a recomendação da IA. Disse que não quer ter de reportar muita coisa: aceita colar só «verde» ou o bloco de erro.
- Ambiente de desenvolvimento (fraco): Windows, Windows PowerShell 5.1, 4 GB de RAM, disco mecânico, sem Docker, VS Code, JDK 21, Maven, PostgreSQL 18 em `C:\Program Files\PostgreSQL\18\bin\psql.exe`. Pasta: `C:\Users\Jose Uqueio\plataforma-academica` (espaço no caminho: pôr sempre entre aspas). A aplicação demora cerca de 245 segundos a arrancar e os testes cerca de 10 minutos localmente, por isso **a verificação principal é o CI do GitHub**.
- Repositório remoto privado: `https://github.com/Alcino-bot/academic-platform-rep` (ramo `main`).
- Usa o Copilot (modo Agent) no VS Code para correr comandos. O terminal do Copilot **não aceita `Read-Host`**, e o Copilot por vezes corrige scripts sem dizer o que mudou.

## 3. A dinâmica de trabalho (repetir)

Cada fatia segue este ciclo:

1. **Desenhar** a fatia e pôr as decisões em aberto ao utilizador, em opções curtas, com a recomendação e o porquê (a ferramenta de opções do chat serve). No máximo duas ou três decisões de cada vez. Registar em `docs/decisoes.md`.
2. **Ler o código real antes de escrever.** Nunca adivinhar assinaturas: o repositório vem anexo.
3. **Construir e testar no ambiente da IA** o que for possível (secção 9). O que não for verificável diz-se, com o motivo.
4. **Sabotar de propósito** as protecções importantes e confirmar que os testes as apanham. Um teste que nunca falha não prova nada. Na lógica pura faz-se no ambiente da IA; nos testes de integração faz-se no CI (ver 5).
5. **Entregar um zip** com caminhos relativos à raiz do repositório, sem BOM, com os documentos actualizados lá dentro, mais os passos do utilizador **com o resultado esperado de cada comando**.
6. **Passos do utilizador, sempre assim:**
   - `git checkout main`, `git pull`, `git checkout -b fatia/<nome>`;
   - `Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\<zip>" -DestinationPath . -Force`, `git status --short` (dizer exactamente que ficheiros devem aparecer);
   - `git add .`, `git commit`, `git push -u origin fatia/<nome>` e ver o resultado no separador Actions (esperado: `Tests run: N, Failures: 0, Errors: 0` no passo Maven e as seis linhas SQL);
   - **sabotagens em ramos descartáveis** (`sabotagem/<nome>-a`, `-b`), cada uma com um comando PowerShell de substituição de texto numa só linha (lê com `[System.IO.File]::ReadAllText`, `.Replace`, grava com `WriteAllText` e `UTF8Encoding($false)`), `git diff --stat` com resultado esperado, `git commit -am`, `git push`, e a lista de testes que **têm** de falhar. Cada sabotagem tem de ser verificada antes de entregue (`grep -c -F` do texto a substituir deve dar 1);
   - limpeza (`git push origin --delete ...`, `git branch -D ...`) e `git checkout main`, `git merge --ff-only fatia/<nome>`, `git push`.
7. **Dizer com franqueza o que não foi verificado.** Um «verde» sem a linha `Tests run:` não prova que os testes correram: o primeiro CI verde durou 44 segundos e foi preciso confirmar. Nunca afirmar que se verificou o que não correu.
8. Fechar a fatia: merge em `main` e documentos já actualizados.

Regras de honestidade que já foram violadas e corrigidas:

- Uma verificação de sintaxe que nunca correu porque faltava `javac` e um `grep` escondeu o erro. **Mostrar sempre o total de erros e quantos sobram depois de filtrar**, e confirmar que a ferramenta existe.
- Um teste com expectativa errada invalidou as sabotagens dessa corrida. Quando a corrida normal falha, as sabotagens dessa corrida não contam.
- Um guião de PowerShell escrito sem poder ser executado. Dizer no momento que não se pode executar e dar um resultado esperado explícito.
- Hipóteses sobre ficheiros do utilizador apresentadas como factos (o README «em UTF-16» era afinal 25 bytes nulos no fim, acrescentados por um `>>` do PowerShell 5.1). Pedir sempre o comando de diagnóstico antes de afirmar a causa.

## 4. Arquitectura e estado técnico

- **Stack:** Java 21, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA (Hibernate 7), JdbcTemplate para consultas específicas, Flyway, PostgreSQL 18, Maven. Frontend **por decidir** (React ou Thymeleaf); a API é JSON com sessão por cookie e CSRF, e serve bem um frontend separado.
- **Monólito modular**, pacote raiz `mz.plataformaacademica`:
  - `comum`: excepções (`ConflitoException` 409, `NaoEncontradoException` 404, `RegraDeNegocioException` 422, `CredenciaisInvalidasException` 401, `AcessoNegadoException` 403) e `PoliticaDePalavraPasse` (`validar`).
  - `config`: `SegurancaConfig` (inclui a regra `/api/administracao/**` exige `PAPEL_GLOBAL_ADMIN_GERAL`).
  - `web`: `EstadoController`, `TratadorDeErros` (`ProblemDetail`).
  - `instituicao`: `Instituicao` (construtores de 3 e 4 argumentos), `TipoInstituicao`, estados e transições, repositórios JPA, `InstituicaoServico` (`criar` com e sem modelo de origem, `obter`, `listar`, `mudarEstado`).
  - `autenticacao`: `AuthController`, `ServicoDeAutenticacao`, `RepositorioDeAutenticacao`, `FiltroDeAutoridades`, `ContextoInstituicao`, `PrincipalPad` (`utilizadorId`, `email`, `nome`, `instituicaoActivaId`).
  - `conta`: `ContaController`, `ServicoDeContas`, `PreparadorDeContas`, `RepositorioDeContas`, `GeradorDeTokens`, `EnviadorDeEmail` e `EnviadorDeEmailPorLog`.
  - `administracao` (novo): `ServicoDoAdministradorInicial` e `AdministradorInicialRunner` (primeiro administrador), `ServicoDeTiposDeInstituicao`, `ServicoDeInstituicoesAdministradas` (cria a instituição a partir de um modelo e copia os tipos de unidade), `AdministracaoController`.
- **Endpoints:** `GET /api/estado`; `GET /api/auth/csrf`; `POST /api/auth/entrar`, `GET /api/auth/eu`, `POST /api/auth/instituicao`, `POST /api/auth/sair`; `POST /api/contas/registar`, `/reenviar-verificacao`, `/activar`; **administração** (só `PAPEL_GLOBAL_ADMIN_GERAL`): `GET` e `POST /api/administracao/tipos-instituicao`, `POST .../tipos-instituicao/{id}/aprovar` e `/rejeitar`, `GET` e `POST /api/administracao/instituicoes`, `POST .../instituicoes/{id}/estado`.
- **Autenticação:** sessão com cookie (`PAD_SESSAO`, HttpOnly, SameSite=Lax, em memória). A instituição activa fica na sessão; a lista de instituições, os papéis e a instituição activa são **revalidados a cada pedido** pelo `FiltroDeAutoridades`. Autoridades: `PAPEL_GLOBAL_<codigo>` (de `atribuicao_global`), `PAPEL_<codigo>` e `VINCULO_<tipo>` (da instituição activa). Um administrador geral entra sem ter vínculo a nenhuma instituição. Bloqueio de conta: 5 tentativas, 15 minutos. Entrada recusada, conta inexistente, suspensa e bloqueada dão a **mesma resposta 401**. Palavra-passe: mínimo 10 caracteres, máximo 72 **bytes**.
- **Registo de contas:** pede só nome e email, cria a conta `PENDENTE` com palavra-passe inutilizável; a palavra-passe define-se ao **activar**, com o token do email. Tokens de 256 bits, guardados como SHA-256, uso único, 24 horas, intervalo mínimo de 2 minutos. Resposta sempre igual. O envio real de email **não existe**: só escreve a ligação no log.
- **Primeiro administrador geral:** variáveis `ADMIN_EMAIL`, `ADMIN_NOME`, `ADMIN_PALAVRA_PASSE` (sem valor por omissão, lidas por `@Value("${ADMIN_EMAIL:}")`). Corre no arranque, fora do perfil `test`, com `pg_advisory_xact_lock`. Só cria se não existir nenhum administrador geral **com conta ACTIVA**. Variáveis parciais, palavra-passe fora da política, email inválido ou email de conta existente fazem o arranque **falhar**. A palavra-passe e o email nunca vão para o log. Depois do primeiro arranque, retirar as variáveis.
- **Administração:** tipos de instituição (propor `PENDENTE` com `proposto_por`; aprovar e rejeitar só a partir de `PENDENTE`, com `SELECT ... FOR UPDATE`, guardam `decidido_por`; a rejeição não guarda motivo porque a tabela não tem a coluna). Instituições: só o administrador geral cria, já `ACTIVA`, a partir de um modelo `ACTIVO` cujo tipo esteja `APROVADA`; os tipos de unidade do modelo são copiados na mesma transacção com a hierarquia remapeada; mudança de estado pelas transições existentes.
- **Propriedades relevantes:** base `${DB_NAME:plataforma_academica_dev}`, utilizador `${DB_USER:postgres}`, palavra-passe `${DB_PASSWORD}` (sem valor por omissão), `seguranca.login.*`, `aplicacao.url-base`, `conta.verificacao.*`. Testes usam o perfil `test` e a base `plataforma_academica_teste`.

## 5. Modelo de dados (26 tabelas, migrações V1 a V20)

Em `src/main/resources/db/migration`. **Nunca se edita uma migração já commitada**: corrige-se com uma migração nova.

**Fase 1:** `tipo_instituicao` (PENDENTE, APROVADA, REJEITADA, INACTIVA; `proposto_por`, `decidido_por`, `data_aprovacao`; sem coluna de motivo), `modelo_organizacional` (estado RASCUNHO, ACTIVO, INACTIVO, pertence a um tipo), `modelo_tipo_unidade` (nivel, pai por chave composta), `instituicao` (PENDENTE, ACTIVA, SUSPENSA, INACTIVA; `modelo_origem_id`), `tipo_unidade` (cópia do molde, com `modelo_tipo_origem_id`), `unidade_organizacional` (árvore). O modelo é um **molde** copiado para cada instituição.

**Fase 2:** `utilizador` (PENDENTE, ACTIVO, SUSPENSO, INACTIVO; hash com pelo menos 20 caracteres; tentativas e bloqueio), `vinculo_institucional` (ESTUDANTE, DOCENTE, FUNCIONARIO, OUTRO; PENDENTE, ACTIVO, SUSPENSO, TERMINADO; conta **global**, vínculo **por instituição**), `permissao` (vazia), `papel` (semeados: `ADMIN_GERAL` GLOBAL; `ADMIN_INSTITUICAO`, `DOCENTE`, `ESTUDANTE`, `REVISOR_BIBLIOTECA` INSTITUCIONAL), `papel_permissao`, `vinculo_papel`, `atribuicao_global`.

**Fase 3A:** `ano_lectivo`, `periodo`, `curso`, `disciplina` (partilhada entre cursos via `curso_disciplina`), `turma`, `docente_turma`, `matricula`, `inscricao`.

**Fase 3B:** `material` (INSTITUCIONAL ou BIBLIOTECA; PENDENTE, PUBLICADO, REJEITADO, RETIRADO; ninguém revê o que submeteu), `ficheiro_material` (só metadados e SHA-256), `material_disciplina`. Função **`materiais_visiveis(utilizador)`**: único ponto de decisão da visibilidade. **Toda a pesquisa de materiais passa por ela.**

**V19 e V20:** bloqueio de conta e `registar_falha_login`; `token_utilizador` e `consumir_token`.

**Padrões do modelo (repetir em tudo o que for novo):**

- **Chave composta para isolamento:** tabela de uma instituição tem `instituicao_id`, `UNIQUE (id, instituicao_id)`, e as ligações usam chaves estrangeiras compostas `(x_id, instituicao_id)`.
- **Tipo garantido pela chave:** coluna fixa (`tipo_vinculo = 'ESTUDANTE'`) incluída na chave estrangeira; o mesmo com `ambito` em `papel`.
- **Unicidade normalizada:** `lower(btrim(nome))` em índices únicos; `NULLS NOT DISTINCT` com pai opcional.
- **Estados** em maiúsculas com `CHECK`, e `CHECK` de coerência estado/campos.
- **Nomes de restrições** explícitos e usados nos testes: `uq_`, `chk_`, `fk_`, `ix_`. `snake_case` sem acentos, `UUID DEFAULT uuidv7()`, `TIMESTAMPTZ`.
- **Regras que atravessam tabelas** ficam no Java (`docs/REGRAS_PENDENTES.md`).

## 6. Convenções de código

- Ficheiros **novos** em UTF-8 sem BOM e fins de linha **LF**. Os ficheiros anteriores do repositório estão em **CRLF** (o Git do utilizador tem `core.autocrlf` activo). Ao editar um ficheiro existente, preservar o CRLF (ler, trocar `\r\n` por `\n`, editar com `assert` de ocorrência única, voltar a `\r\n`) para o diff ficar mínimo. Se o utilizador quiser uniformizar, é um commit à parte com `.gitattributes`.
- No Windows PowerShell 5.1, `Set-Content -Encoding utf8` e `>>` estragam a codificação: usar `[System.IO.File]::WriteAllText(caminho, texto, (New-Object System.Text.UTF8Encoding($false)))`.
- **Sem comentários** no código, SQL ou YAML gerados.
- Java em português: `Servico`, `Repositorio`, `Controller`, `Exception`. Entidades JPA com `@UuidGenerator(style = VERSION_7)`.
- Mensagens ao utilizador em português, sem revelar se uma conta existe.
- O identificador da instituição **vem sempre da sessão** (`ContextoInstituicao`), nunca do corpo ou do URL. (A administração global é a excepção explícita: opera sobre instituições, e o acesso é por papel global.)
- Cada consulta nova com dados de instituições leva um teste que prova o isolamento.
- O serviço de entrada **não é `@Transactional`**.

## 7. Testes

- **Java: 94 testes.** `EstadoInstituicaoTest` (4), `InstituicaoServicoTest` (11), `AutenticacaoTest` (15), `ContasTest` (15), `GeradorDeTokensTest` (5), `PoliticaDePalavraPasseTest` (8), `AdministradorInicialTest` (15), `AdministracaoTest` (21). `mvn test "-DargLine=-Xmx512m"`. Integração com `@SpringBootTest`, `@ActiveProfiles("test")`, `@Transactional` (desfeitos no fim) e `MockMvc` construído à mão: `MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build()`. Para sessão: `POST /api/auth/entrar` com `csrf()` e `(MockHttpSession) resultado.getRequest().getSession(false)`; depois `.session(sessao).with(csrf())`.
- **SQL: 235 verificações** em `src/test/sql`: `T_fase1` (21), `T_fase2` (38), `T_fase3a` (58), `T_fase3b` (52), `T_autenticacao` (35), `T_contas` (31). Cada ficheiro é `BEGIN ... ROLLBACK`, com auxiliares `pg_temp.*` e saídas `NOTICE:  OK: ...` e `TESTE FALHOU: ...`. `scripts/ci-sql.sh` corre os seis e confere os números (a sabotagem trocando um número esperado prova que o passo SQL do CI falha quando deve).
- **Guião real:** `scripts/verificar-fluxo.ps1` (24 verificações). Ainda não cobre o administrador inicial nem a administração; acrescentar quando houver interface ou numa fatia própria.
- **CI:** `.github/workflows/testes.yml`, PostgreSQL 18 num contentor, `actions/checkout@v6`, `actions/setup-java@v5`, corre `mvn` e depois os testes SQL. Gatilho sem filtro de ramo (corre em todos os ramos, que é o que permite as sabotagens).
- Padrão de teste: um por regra, a verificar **o nome da restrição que falhou**, casos felizes e infelizes; cada fatia acrescenta ao total esperado em `ESTADO.md` e `BACKLOG.md`.

## 8. Armadilhas já encontradas (não repetir)

**Spring Boot 4 e Spring Security 7**
- O *starter* web chama-se `spring-boot-starter-webmvc`; Flyway exige `spring-boot-starter-flyway` e `flyway-database-postgresql`; JDBC exige `spring-boot-starter-jdbc`.
- Pacotes das anotações de teste mudaram: **construir o `MockMvc` à mão**.
- Um `OncePerRequestFilter` **não pode ser bean**: instancia-se dentro do `SecurityFilterChain`.
- Com CSRF e frontend separado, `CsrfTokenRequestAttributeHandler` (token simples) e renovar o token ao entrar. Sem `httpBasic` nem `formLogin`; um `UserDetailsService` que recusa sempre.
- **Não** usar `@Transactional` onde um erro tem de deixar rasto gravado (contador de falhas de login).
- Verificar nomes de classes do Spring Boot 4 na documentação (pesquisa na web) antes de os usar.

**JPA, Hibernate e SQL (novas)**
- **Hibernate actualiza todas as colunas** de uma entidade modificada. Gravar uma coluna por `UPDATE` à parte e depois mudar o estado pela entidade **sobrescreve-a com o valor antigo** (foi o risco com `modelo_origem_id`). Colunas novas entram pela entidade.
- Dentro de um teste `@Transactional`, um erro de base de dados (violação de restrição) **aborta a transacção do PostgreSQL**: nenhuma consulta pode vir depois, no mesmo teste. Preferir `INSERT ... ON CONFLICT DO NOTHING RETURNING`, que não gera erro.
- Alterações feitas por JPA só chegam à base no `flush`: nos testes, `em.flush()` **antes** de verificar por `JdbcTemplate`, e antes de um pedido que vai falhar com 4xx (a transacção fica marcada para rollback).
- `SELECT ?, id` com parâmetro sem tipo pode falhar: usar `CAST(? AS uuid)`.
- `queryForObject` devolve `null` para valor nulo. `jdbc.query(sql, (rs, linha) -> ...)` com dois parâmetros no lambda evita a ambiguidade entre `RowMapper` e os outros.
- O BCrypt usa só os primeiros 72 **bytes**; validar no registo.
- Um `ApplicationRunner` de arranque deve ter `@Profile("!test")`, senão corre (e grava, fora da transacção do teste) em cada contexto de teste.

**PowerShell, Git e Windows**
- Variáveis (`$psql`, `$env:DB_PASSWORD`) só existem no terminal onde foram definidas; argumentos `-D...` do Maven entre aspas; `psql -c` com acentos estraga a codificação (usar `-f` com `PGCLIENTENCODING=UTF8`); caminho com espaço exige aspas e `&`.
- `>>` e `Out-File` do PowerShell 5.1 gravam UTF-16: já acrescentaram bytes nulos ao README e o Git passou a tratá-lo como binário. Diagnóstico: `git ls-files --eol <ficheiro>` (`i/-text` = binário).
- `git add .` com `autocrlf` emite avisos «LF will be replaced by CRLF»: são normais.
- **Nunca pôr a palavra-passe no texto de um comando.**

**Base de dados**
- A base usa `uuidv7()` (PostgreSQL 18). Ambientes de teste com PostgreSQL 16 precisam de uma função de substituição criada só nesse ambiente.
- Testes de `materiais_visiveis` têm de cobrir vínculo terminado, vínculo suspenso, conta suspensa, instituição pendente, material retirado e visitante anónimo.
- Ao gerar ficheiros de teste por script Python, não usar `%` do Python em textos com `%`: usar `.replace`.

**Integração contínua**
- Versões antigas das acções (`checkout@v4`, `setup-java@v4`) deixaram de funcionar (Node 20). Não filtrar o gatilho a `main`.
- Confirmar sempre a linha `Tests run:` no passo Maven, não só o «Sucesso».

## 9. O que o ambiente de uma IA pode verificar (confirmar no início)

Observado na última sessão (confirmar no novo ambiente, que pode diferir):

- **Existia:** Java 21 só como runtime, Python 3, Git, zip. **Não existia:** `javac` instalado, Maven, PostgreSQL. **Sem rede** (nem Maven Central). O repositório chega por anexo.
- **Truque que funcionou:** o runtime tinha o módulo `jdk.compiler`, logo `java -m jdk.compiler/com.sun.tools.javac.Main -d <saida> -encoding UTF-8 @lista.txt` compila. Confirmar com `java --list-modules | grep compiler`.
- **Verificação do lado Java, por camadas:**
  1. Criar **peças de substituição** mínimas das anotações e tipos do Spring/JPA/Jakarta usados (interfaces e anotações vazias), copiar as classes reais de `comum` e as do projecto de que o código novo depende, e compilar o código novo. Esperado: 0 erros.
  2. Para o ficheiro de testes (JUnit, AssertJ, Spring Test e Logback não existem): compilar e mostrar **o total de erros e quantos não são «falta o pacote» ou «cannot find symbol»**. Esperado: 0 fora dessa categoria (não há erros de sintaxe).
  3. **Harness** de lógica: um `JdbcTemplate` simulado (subclasse da peça de substituição) que responde por pedaço do SQL, repositórios simulados, e cenários que imprimem `OK`/`FALHOU`. Depois **sabotar** o código (`sed` de uma condição, de uma chamada) e confirmar que o harness falha onde deve.
  4. Higiene dos ficheiros: contar `\r\n` e `\n` soltos e BOM com Python (não com `grep $'\r'`, que não funciona em `sh`).
- **Não é possível verificar aqui:** SQL real, restrições, arranque do Spring, testes de integração, regras de segurança do `SecurityFilterChain`. Isso é o CI. Dizê-lo sempre.
- Se `pgserver` (PostgreSQL 16) ou outra ferramenta existir, o servidor morre entre chamadas: correr tudo dentro de um único processo Python e criar `uuidv7()` de substituição só nesse ambiente. Consultas partilhadas entre Java e testes SQL devem sair do mesmo texto.
- Se a nova IA **não** puder executar código, deve dizê-lo e ser mais conservadora, com verificação de sintaxe e resultados esperados explícitos em cada entrega.

## 10. O que fazer a seguir

Ler `docs/BACKLOG.md`. Estado a 20 de Outubro de 2026: **Fase A fechada** (repositório remoto, CI verde, documentos). Na **Fase B** estão feitos o primeiro administrador geral e a administração de tipos e instituições. Falta, por esta ordem:

- **6b. Modelos organizacionais pela API:** criar um modelo (com os seus tipos de unidade, hierarquia validada: nível do filho = nível do pai + 1) e activá-lo. Sem isto, numa base nova não há modelos, e os tipos e modelos têm de ser inseridos por SQL. Considerar também um script de semente para o piloto.
- **7. Pedidos de vínculo e atribuição de papéis:** a pessoa pede um vínculo (`PENDENTE`), o `ADMIN_INSTITUICAO` aprova, atribui papéis institucionais. Aqui entra o primeiro `ADMIN_INSTITUICAO` (como se designa o primeiro de cada instituição: decisão a pôr ao utilizador).
- **8. Recuperação de palavra-passe** (reutiliza `token_utilizador`, resposta sempre igual).
- **9. Envio real de email** atrás de `EnviadorDeEmail` (depende de escolher o fornecedor).
- **10. Interface mínima** (depende de decidir React ou Thymeleaf). Recomendação já dada: decidir pelo que o utilizador domina e manter a API.
- **Fase C (biblioteca moderada):** submissão com ficheiros, revisão e aprovação, publicação, pesquisa textual sempre atrás de `materiais_visiveis`, descarga segura, retirada. Depois o **piloto**, antes das fases D (vida académica e trabalhos), E (comunicação e auditoria), F (IA) e G (produção).
- **Fase G0 (antes do piloto):** hospedagem, HTTPS, cookie `secure`, cópias de segurança com restauro testado, monitorização, limitação por endereço, política de privacidade.

**Para chegar mais depressa ao objectivo (recomendações):**

1. **Decidir a interface agora.** Sem ela nada é demonstrável, e é a maior distância até um produto visível. Uma interface mínima (entrar, escolher instituição, administração) pode vir logo a seguir ao item 7.
2. **Juntar fatias pequenas** quando partilham desenho (6b e a semente do piloto; 7 e a atribuição de papéis), mantendo cada ramo com a sua sabotagem.
3. **Validar requisitos em paralelo** com duas instituições reais (tipos diferentes), porque os modelos organizacionais são hipóteses. Isto não gasta código e evita refazer a Fase D.
4. **Pôr a decisão de produto «biblioteca pública visível a visitantes?» e a escolha de email e hospedagem** num só momento, em opções curtas, para não bloquearem a Fase B e a G0.
5. **Estimativa original** (10 horas por semana de uma pessoa, ±50%): até ao piloto 13 a 19 semanas; visão completa 9 a 13 meses. A IA a produzir e o CI a verificar encurta isto, mas não elimina a incerteza.

Dívida pequena pendente: uniformizar fins de linha (`.gitattributes`), estender `scripts/verificar-fluxo.ps1` ao administrador inicial e à administração, mudar a palavra-passe da base se ainda não foi (foi mudada antes do primeiro `push`; confirmar que nada com segredos ficou versionado).

## 11. Definição de «fatia pronta»

- A decisão está escrita em `docs/decisoes.md`.
- Há migração nova (se houve alteração à base), com nomes de restrições explícitos e testes SQL que verificam o nome da restrição.
- Há testes Java para as regras novas e testes de isolamento entre instituições onde se tocou em dados de instituições.
- As protecções críticas foram sabotadas, e **o CI ficou vermelho nos testes certos**.
- O CI do ramo da fatia está verde com a linha `Tests run: N` esperada.
- O zip não tem BOM; os ficheiros novos são LF e os editados mantêm o CRLF; os passos têm o resultado esperado de cada comando.
- Ficou dito o que **não** foi verificado.
- Merge em `main` feito, com `docs/ESTADO.md`, `docs/BACKLOG.md` e `docs/decisoes.md` já actualizados dentro da fatia.

## 12. Pedido inicial para colar na nova IA

> Vais continuar um projecto já em curso, a Plataforma Académica Digital, com um estudante que fala português (Moçambique). Lê integralmente o ficheiro CONTEXTO_PARA_ASSISTENTES.md anexo e os de docs/ (ESTADO, BACKLOG, decisoes, REGRAS_PENDENTES). Segue a dinâmica da secção 3: desenhas, constróis e testas no teu ambiente o que for possível, sabotas as protecções críticas, entregas um zip com os documentos já actualizados e os passos com resultados esperados, e dizes com franqueza o que não verificaste. O CI do GitHub é o verificador dos testes de integração.
>
> Antes de escreveres código: (1) diz-me o que o teu ambiente consegue executar (Java, `jdk.compiler`, Maven, PostgreSQL, rede) e como isso muda a verificação; (2) resume em dez linhas o que percebeste do estado e do próximo passo, para eu corrigir mal-entendidos. Já sei que o repositório está em `main` com o CI verde e a Fase B a meio. Responde de forma directa e curta, sem lisonja, e põe as decisões em opções curtas com a tua recomendação. Sem comentários no código. Lê o código real do repositório anexo antes de escrever qualquer classe. A primeira tarefa é o item 6b do `docs/BACKLOG.md` (modelos organizacionais pela API).

## 13. Manutenção deste ficheiro

A parte estável (secções 1 a 9 e 11) muda raramente. Actualiza-a quando mudar uma decisão de arquitectura, uma convenção ou se encontrares uma armadilha nova. O estado e o trabalho por fazer vivem em `docs/ESTADO.md` e `docs/BACKLOG.md`: não os dupliques aqui, excepto o resumo da secção 10 quando uma fase fechar.
