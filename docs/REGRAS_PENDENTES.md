# Regras pendentes

Regras que atravessam várias tabelas e foram deixadas para o Java de propósito. Cada uma só se risca quando estiver implementada e testada.

## Estrutura académica

- [ ] A disciplina da turma pertence ao curso da matrícula.
- [ ] O período da turma pertence ao ano lectivo da matrícula.
- [ ] O período está dentro das datas do ano lectivo.
- [ ] Os anos lectivos da mesma instituição não se sobrepõem.
- [x] Na hierarquia de tipos, o nível do filho é o do pai mais um. (Modelos organizacionais: derivado ao criar e revalidado ao activar. Os tipos de unidade das instituições só nascem por cópia do modelo.)
- [ ] O tipo da unidade pai é o tipo pai do tipo da unidade filha.
- [ ] A capacidade da turma limita as inscrições.

## Biblioteca

- [ ] Quem revê um material tem o papel de revisor da biblioteca.
- [ ] Quem submete tem vínculo activo no momento da submissão.
- [ ] Mudar o âmbito ou substituir um ficheiro depois de publicado reabre a revisão.
- [ ] Toda a pesquisa de materiais passa por `materiais_visiveis`.

## Administração

- [x] Quem decide sobre um tipo de instituição é administrador geral.

## Decisões de produto ligadas a regras

- [ ] A biblioteca pública é visível a visitantes sem conta? Hoje só a contas activas com sessão.
- [ ] O material institucional também exige aprovação? Hoje sempre exige.
