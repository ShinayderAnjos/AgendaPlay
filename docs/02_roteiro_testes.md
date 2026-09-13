# Roteiro de testes — AgendaPlay 0.1.0

Responsáveis pela execução e aceitação: Geanderson e Lázaro, com revisão da modelagem por Diego.

## Preparação

Inicie o sistema conforme o README e abra http://localhost:8080. Use duas janelas de navegador independentes (normal e privativa, ou navegadores diferentes) para comparar sessões.

Cadastre dados fictícios exclusivos deste ambiente:

| Perfil | Nome sugerido | E-mail de teste | CPF de teste |
| --- | --- | --- | --- |
| Proprietário A | Proprietário de Teste A | proprietario.a@example.test | Pode ficar vazio |
| Proprietário B | Proprietário de Teste B | proprietario.b@example.test | Pode ficar vazio |
| Cliente A | Cliente de Teste A | cliente.a@example.test | 529.982.247-25 |
| Cliente B | Cliente de Teste B | cliente.b@example.test | 111.444.777-35 |

Esses identificadores são exemplos sintéticos para exercitar validação; não representam identidade verificada. Use senhas próprias de teste com no mínimo 8 caracteres. Não há contas padrão no banco principal.

## Casos de teste

| ID | História | Ação | Resultado esperado |
| --- | --- | --- | --- |
| T01 | US01 | Cadastrar cliente com todos os dados válidos | Conta criada, retorno ao login e perfil CLIENTE |
| T02 | US01 | CPF inválido, e-mail malformado, senha curta ou confirmação diferente | Cadastro bloqueado e mensagem compreensível |
| T03 | US01/02 | Repetir e-mail em maiúsculas ou CPF já usado | Duplicidade bloqueada |
| T04 | US02 | Cadastrar proprietário sem CPF | Conta criada como PROPRIETARIO |
| T05 | US03 | Entrar com senha incorreta; depois correta | Erro genérico na primeira tentativa; sessão na segunda |
| T06 | US04 | Sair e tentar abrir /reservas diretamente | Novo login obrigatório |
| T07 | US03 | Cliente tenta /proprietario/quadras; proprietário tenta /cliente/quadras | Acesso negado |
| T08 | US05 | Proprietário A cadastra quadra ativa a R$ 90/h | Quadra aparece somente na gestão de A e no catálogo do cliente |
| T09 | US05 | Informar valor negativo | Operação bloqueada |
| T10 | US05/06 | Proprietário B tenta editar ID da quadra de A | Acesso negado, sem mudanças |
| T11 | US06 | Editar nome/endereço/valor | Catálogo mostra os novos dados |
| T12 | US07 | Disponibilizar data futura, das 08:00 às 18:00 | Intervalo aparece na consulta do cliente |
| T13 | US07 | Repetir/sobrepor disponibilidade, data passada ou fim menor que início | Operações bloqueadas |
| T14 | US08/09 | Cliente consulta quadra e horários | Dados públicos; somente intervalos ativos, futuros e livres |
| T15 | US10 | Cliente A reserva 10:00–11:30, a R$ 90/h | Confirmação com quadra, data, horário, status e R$ 135,00 |
| T16 | US11 | Cliente B tenta 10:00–11:30 ou 11:00–12:00 | Conflito bloqueado; escolha de novo intervalo possível |
| T17 | US11 | Duas sessões tentam confirmar o mesmo intervalo | No máximo uma confirmação |
| T18 | US11 | Reservar 11:30–12:00 após reserva até 11:30 | Permitido, sem sobreposição |
| T19 | US12/13 | Consultar reservas nos quatro perfis de teste | Cliente vê só as suas; proprietário só as de suas quadras |
| T20 | US13 | Proprietário filtra quadra e data | Lista contém somente os resultados do filtro |
| T21 | US14 | Cliente responsável ou dono cancela reserva futura | Status CANCELADA; histórico mantido; horário liberado |
| T22 | US14 | Outro usuário tenta cancelar a reserva | Acesso negado |
| T23 | US06 | Inativar quadra com reserva confirmada | Sai do catálogo e não recebe novas reservas; histórico preservado |
| T24 | US07 | Inativar disponibilidade que possui reserva confirmada | Bloqueado até cancelar a reserva |
| T25 | Frontend | Repetir navegação em celular ou largura de 390 px | Conteúdo legível, botões acessíveis e sem rolagem horizontal |
| T26 | Banco | Reiniciar o servidor e abrir as consultas | Dados permanecem; Flyway não recria nem duplica tabelas |

## Como registrar um problema

Copie e preencha este modelo em `docs/registro_testes.md`:

- Caso de teste:
- Data e responsável:
- Perfil utilizado (sem senha):
- Passos para reproduzir:
- Resultado esperado:
- Resultado observado:
- Captura de tela, sem dados privados:
- Impacto: bloqueia o fluxo / dificulta / visual:
- Resultado do reteste:

Registre aprovado/reprovado para cada caso. Não considere a Sprint aceita apenas porque os testes automatizados passaram.
