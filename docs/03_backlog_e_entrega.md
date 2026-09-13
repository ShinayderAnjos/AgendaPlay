# Backlog e incremento entregue

| História | Implementação | Evidência técnica |
| --- | --- | --- |
| US01 — Cadastrar cliente | Formulário validado, CPF, e-mail único, senha e confirmação | Testes de cadastro válido/inválido, duplicidade e hash |
| US02 — Cadastrar proprietário | Rota específica, perfil definido pelo servidor | Teste sem CPF e separação de perfis |
| US03 — Login | E-mail/senha, sessão, erro genérico, rotas protegidas | Login real e testes de autorização |
| US04 — Logout | POST com CSRF e invalidação da sessão | Teste da sessão invalidada |
| US05 — Cadastrar quadra | Proprietário autenticado, campos obrigatórios e valor não negativo | Teste de cadastro com tentativa de forjar proprietário |
| US06 — Gerenciar quadra | Consulta própria, edição e inativação | Teste de acesso alheio e preservação de histórico |
| US07 — Disponibilidade | Data e intervalo futuro, sem sobreposição, inativação | Testes de horário inválido, duplicidade e propriedade |
| US08 — Consultar quadras | Catálogo de quadras ativas, sem dados privados | Teste de catálogo e inativação |
| US09 — Consultar horários | Intervalos livres calculados no servidor | Teste de recorte após reserva |
| US10 — Reservar | Revalidação transacional e preço proporcional | Teste por serviço, formulário e navegador |
| US11 — Bloquear conflitos | Bloqueio transacional e restrição GiST | Teste concorrente, sobreposições e inserção direta |
| US12 — Minhas reservas | Lista pelo cliente da sessão, incluindo canceladas | Teste de isolamento |
| US13 — Agenda do proprietário | Reservas das próprias quadras, filtros por quadra/data | Teste de filtros e isolamento |
| US14 — Cancelar | Responsável ou dono, sem exclusão física | Teste de autorização, histórico e liberação de horário |

US14 utiliza regra provisória para testes: cancelamento antes do início, sem antecedência extra. A definição final depende de validação com os usuários.

## Critérios técnicos verificados

- Projeto compila com Java 21 e Maven Wrapper.
- Migração aplicada no PostgreSQL 17 isolado de testes.
- Testes integrados com banco real, incluindo solicitações concorrentes.
- Navegação real em navegador e inspeção visual de desktop/celular.
- SQL documentado em `.txt` e migração versionada.
- Código em camadas, roteiro de testes e instruções de execução.

## Antes da aceitação da equipe

- Executar o roteiro manual com Geanderson/Lázaro e registrar feedback.
- Conferir as diferenças de implementação com Diego em `01_arquitetura_e_modelagem.md`.
- Validar a política final de cancelamento e regras de duração/preço com o Product Owner.
- Consolidar correções, retestes e evidências para a apresentação do incremento.

Nenhuma mensagem foi enviada à equipe e nenhuma aprovação de usuário foi presumida.
