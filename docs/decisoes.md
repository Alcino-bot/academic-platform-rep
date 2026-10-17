# Decisões de arquitectura
- Núcleo comum com isolamento lógico por instituição (instituicao_id + chaves estrangeiras compostas).
- Configuração institucional por dados: o modelo organizacional é copiado para cada instituição, não ligado.
- Biblioteca com estado pendente e aprovação obrigatória antes da publicação.
- IA como assistente; decisões críticas ficam com regras e pessoas.
- Migrações em ficheiros V*.sql (Flyway); a base de desenvolvimento é descartável e recriável.
- UTF-8 sem BOM em todos os ficheiros.