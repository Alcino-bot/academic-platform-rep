# Estado do projecto

Última actualização: 21 de Outubro de 2026. Última verificação com a aplicação real: 19 de Outubro de 2026.

## Resumo

A fundação técnica está concluída e verificada. Ainda não há interface. Existe o primeiro administrador geral, a administração de tipos, modelos organizacionais e instituições por API, e o fluxo de pedidos de vínculo com aprovação. O ciclo tipo, modelo e instituição já se faz só pela aplicação.

## O que existe

- **Modelo de dados:** 21 migrações Flyway (V1 a V21), 26 tabelas, 3 funções (`materiais_visiveis`, `registar_falha_login`, `consumir_token`). O isolamento entre instituições é garantido pela base (chaves estrangeiras compostas).
- **Backend:** Spring Boot 4.1.0, Java 21. Instituições com estados e transições; autenticação por sessão com cookie; instituição activa escolhida ao entrar; papéis revalidados a cada pedido; bloqueio de conta (5 tentativas, 15 minutos); protecção CSRF; registo com verificação por email.
- **Primeiro administrador geral:** criado no arranque a partir de `ADMIN_EMAIL`, `ADMIN_NOME` e `ADMIN_PALAVRA_PASSE` (sem valores por omissão). Só cria se não existir nenhum administrador geral activo. Depois do primeiro arranque, retirar as variáveis.
- **Administração (só `PAPEL_GLOBAL_ADMIN_GERAL`):** `GET` e `POST /api/administracao/tipos-instituicao`, `POST .../tipos-instituicao/{id}/aprovar` e `/rejeitar`, `GET` e `POST /api/administracao/instituicoes` (cria já ACTIVA a partir de um modelo activo e copia os tipos de unidade) e `POST .../instituicoes/{id}/estado`.
- **Modelos organizacionais (só `PAPEL_GLOBAL_ADMIN_GERAL`):** `GET /api/administracao/modelos-organizacionais?estado=`, `GET .../{id}`, `POST` (cria em RASCUNHO com a árvore completa de tipos de unidade; o nível de cada tipo é derivado do pai), `POST .../{id}/activar` e `POST .../{id}/inactivar`. Sem edição: para mudar um modelo, cria-se outro.
- **Vínculos:** `POST /api/vinculos/pedidos` e `GET /api/vinculos/meus` (qualquer pessoa autenticada). Administrador da instituição activa (`PAPEL_ADMIN_INSTITUICAO`): `GET /api/instituicao/vinculos?estado=`, `POST .../{id}/aprovar`, `POST .../{id}/rejeitar` (com motivo), `POST .../{id}/papeis` e `POST .../{id}/papeis/{papel}/revogar`. Administrador geral: `POST /api/administracao/instituicoes/{id}/administradores` designa o administrador de uma instituição.
- **Verificação:** 186 testes Java, 251 verificações SQL (21, 38, 58, 52, 35, 31 e 16 por ficheiro; o 6b não tem migração nem testes SQL novos) e o guião `scripts/verificar-fluxo.ps1` (24 verificações, 0 falhas; ainda não cobre o 6b).

## O que falta

- Interface de utilizador (React ou Thymeleaf: decisão em aberto).
- Recuperação de palavra-passe e envio real de email (hoje só existe o modo registo).
- Biblioteca em Java: submissão, ficheiros, revisão, pesquisa, descarga.
- Regras académicas em Java (ver `REGRAS_PENDENTES.md`), comunicação, IA, hospedagem, cópias de segurança.

## Como se verifica

- `mvn test`: esperado 186 testes, 0 falhas.
- Testes SQL: esperado 21, 38, 58, 52, 35, 31 e 16 verificações `OK` (o script `scripts/ci-sql.sh` confere os números).
- `scripts/verificar-fluxo.ps1`: esperado `Total de verificacoes com falha: 0`.
- A integração contínua do GitHub corre os dois primeiros em cada alteração.

## Problemas conhecidos

- O arranque da aplicação demora cerca de 245 segundos no computador de desenvolvimento (4 GB de RAM, disco mecânico).
- O envio de email só escreve a ligação no registo da aplicação. Não usar assim com pessoas reais.
- O cookie de sessão não está marcado como seguro (`secure=false`), as sessões ficam em memória e não há limitação por endereço. Aceitável em desenvolvimento, não em produção.
- A palavra-passe da base de dados apareceu em texto nos registos de ferramentas. Considerar comprometida e mudar.

## Próximo passo

Fase B: recuperação de palavra-passe (item 8) ou a decisão da interface (item 10). Ficou por fazer, de propósito, o guião de ciclo completo e a semente do piloto (item 6c).
