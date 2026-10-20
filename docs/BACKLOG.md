# Backlog

Ordem de prioridade. Cada item tem o critério de pronto.

## Fase A: proteger e consolidar

1. Repositório remoto privado no GitHub com todo o histórico. Pronto quando: um clone limpo noutra pasta existe e abre.
2. Mudar a palavra-passe da base de dados e guardá-la fora do repositório. Pronto quando: a antiga deixou de funcionar.
3. Integração contínua verde. Pronto quando: a execução "Testes" do GitHub termina com sucesso, com 58 testes Java e as 6 verificações SQL.
4. Clone limpo noutra máquina arranca e passa nos testes seguindo só o `README.md`.

## Fase B: plataforma operável

5. Primeiro administrador geral criado por configuração no arranque. Pronto quando: existe um administrador que entra sem a base ser tocada à mão.
6. Administração: criar instituições, aprovar ou rejeitar tipos de instituição, mudar estados. Pronto quando: um administrador faz o ciclo completo só pela aplicação.
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

Fase D (vida académica e trabalhos), fase E (comunicação e auditoria), fase F (inteligência artificial) e fase G (produção). Ver a análise estratégica.

## Decisões em aberto

React ou Thymeleaf; fornecedor de email; hospedagem do piloto; biblioteca pública visível a visitantes sem conta; enquadramento de protecção de dados e direitos de autor; instituição piloto e quem é o dono do produto.
