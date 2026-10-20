# Backlog

Ordem de prioridade. Cada item tem o critério de pronto.

## Fase A: proteger e consolidar

1. (feito) Repositório remoto privado no GitHub com todo o histórico. Pronto quando: um clone limpo noutra pasta existe e abre.
2. (feito) Mudar a palavra-passe da base de dados e guardá-la fora do repositório. Pronto quando: a antiga deixou de funcionar.
3. (feito) Integração contínua verde. Pronto quando: a execução "Testes" do GitHub termina com sucesso, com os testes Java e as 6 verificações SQL.
4. (feito) Clone limpo noutra máquina arranca e passa nos testes seguindo só o `README.md`.

## Fase B: plataforma operável

5. (feito) Primeiro administrador geral criado por configuração no arranque. Pronto quando: existe um administrador que entra sem a base ser tocada à mão.
6. (feito) Administração de tipos de instituição (propor, aprovar, rejeitar) e de instituições (criar a partir de um modelo, listar, mudar estado).
6b. Modelos organizacionais pela API: listar e consultar modelos, criar um rascunho, acrescentar tipos de unidade com hierarquia validada e activar após aprovação do tipo de instituição. Implementação entregue; aguarda CI verde e sabotagens verificadas. Pronto quando: um administrador faz o ciclo completo (tipo, modelo, instituição) só pela aplicação.
7. Pedidos de vínculo e aprovação pelo administrador da instituição. Pronto quando: uma pessoa nova regista-se, pede um vínculo, é aprovada e entra numa instituição.
8. Recuperação de palavra-passe (reutiliza a tabela de tokens). Pronto quando: o fluxo completo está testado, incluindo a expiração e o uso único.
9. Envio real de email atrás da interface existente. Pronto quando: a mensagem chega a uma caixa de correio real. Depende de escolher o fornecedor.
10. Interface mínima: entrar, escolher a instituição, administração. Depende de decidir React ou Thymeleaf.

## Fase C: biblioteca moderada

11. Submissão de materiais com ficheiros.
12. Revisão e aprovação pelo revisor da biblioteca.
13. Publicação institucional e na biblioteca pública.
14. Pesquisa textual sempre atrás de `materiais_visiveis`, com testes de regressão de visibilidade.
15. Descarga segura e retirada de material.

## Depois do piloto

Fase D (vida académica e trabalhos), fase E (comunicação e auditoria), fase F (inteligência artificial) e fase G (produção). A fase F está excluída por decisão do utilizador: quando o backlog chegar a esse ponto, avisar e não implementar. Ver a análise estratégica.

## Decisões em aberto

React ou Thymeleaf; fornecedor de email; hospedagem do piloto; biblioteca pública visível a visitantes sem conta; enquadramento de protecção de dados e direitos de autor; instituição piloto e quem é o dono do produto.
