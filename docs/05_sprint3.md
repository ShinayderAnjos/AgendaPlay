# Sprint 3 — implementação e validação

Data: 21/09/2026. Projeto atualizado na pasta principal.

## Requisitos entregues

| Requisito | Comportamento implementado |
| --- | --- |
| Senha forte | 8–60 caracteres, maiúscula, minúscula, número e símbolo. Validação no cadastro e serviço; feedback imediato na tela; limite de 72 bytes do BCrypt mantido. |
| Documento do proprietário | CPF ou CNPJ obrigatório no cadastro, com dígitos verificadores e normalização. CNPJ numérico ou alfanumérico, com letras preservadas. Cliente continua usando CPF. |
| Filtros para clientes | Modalidade, localização, estabelecimento, data, início/fim, preço máximo por hora e somente quadras com horários livres. Os filtros consultam os preços efetivos dos horários. |
| Localização pelo mapa | Clique no mapa, localização do navegador mediante autorização, coordenadas editáveis e link para Google Maps. Coordenadas persistidas no estabelecimento e na quadra. |
| Estabelecimentos | Cadastro, edição, vínculo obrigatório da quadra, seleção apenas de estabelecimentos próprios e catálogo por estabelecimento. |
| Disponibilidade recorrente | Modelos manhã, tarde, noite e fim de semana; dias selecionáveis; duração, início/fim, intervalo e preço configuráveis; prévia antes de salvar. |
| Renovação | Gera até o último dia do mês seguinte, renova de hora em hora com o servidor em execução e também ao consultar horários. Retoma após períodos com o servidor desligado. Sem duplicar horários. |
| Horário específico/avulso | Continua disponível, com preço e intervalo próprios. Para substituir uma ocorrência do padrão, inative o horário gerado e cadastre o avulso; a ocorrência inativada não reaparece. |
| Tolerância | Padrão de 10 minutos, configurável de 0 a 1440 minutos por quadra/padrão/avulso. Aplicada a reservas parciais, aos dois lados de reservas existentes e ao atravessar a meia-noite. A restrição no banco também impede conflitos. |
| Preços por dias/horários | Padrões distintos e horários avulsos guardam preços próprios. Reserva parcial cobra proporcionalmente à duração, sem cobrar o intervalo de descanso. O cliente vê a estimativa; o servidor confirma o valor. |
| Cancelamentos | Notificação persistente para a outra parte, apresentada ao entrar e na página inicial. Permanece até marcar como lida. Repetir cancelamento não duplica o aviso. |
| Agenda do proprietário | Dia anterior/hoje/próximo dia, filtro por estabelecimento e quadra, ordenação por quadra ou horário, reservas e horários livres. Dados de outros proprietários não são expostos. |

## Decisões de funcionamento

- Alterar preço ou tolerância da quadra afeta novas disponibilidades. Horários já publicados e reservas confirmadas preservam os valores registrados.
- Horários são no mesmo dia e no fuso de Brasília. O intervalo de descanso pode avançar para o dia seguinte.
- Desativar um padrão interrompe a renovação e retira horários futuros vazios. Janelas que já contêm reservas confirmadas são mantidas, junto com o histórico.
- Padrões ativos sobrepostos nos mesmos dias são rejeitados. Horários avulsos já cadastrados têm prioridade na geração.
- Quadras antigas recebem automaticamente um estabelecimento principal do próprio proprietário. Revise e renomeie esses estabelecimentos após atualizar.
- Contas antigas de proprietário sem documento são preservadas na migração; a obrigatoriedade vale para novos cadastros. Nenhum documento é inventado para registros antigos.
- Reservas antigas conservam tolerância zero, preservando reservas adjacentes já confirmadas.
- O CSS restaurado `app.css` não foi alterado (SHA-256: 054D092B373E414E1DA2313100FC1427549267AA180D92814008580EAF448D1C).

## Testes executados

**42 testes automatizados, sem falhas ou erros**, com Spring Boot, MockMvc e PostgreSQL 17 isolado na porta 55432. A suíte anterior foi preservada e ajustada às regras obrigatórias novas.

Cobertura: login/logout, sessão, CSRF, isolamento entre perfis e proprietários, formulários, documento e senha, valores inválidos, datas passadas, reserva parcial, conflitos, concorrência de reservas, preço efetivo e histórico, tolerância 0/10/15 minutos, virada da meia-noite, geração semanal, renovação com relógio avançado um mês, renovações simultâneas, desativação, exceção avulsa, filtros e notificações.

**Teste real de navegador no Microsoft Edge, desktop 1365×900 e celular 390×844:** cadastro/login dos dois perfis, estabelecimento com ponto no mapa, quadra vinculada, modelo semanal, reserva de 30 minutos por R$ 60,00 sobre tarifa de R$ 120,00/hora, liberação apenas após 10 minutos, agenda, cancelamento, aviso no próximo login, marcação como lida e desativação. Sem erros JavaScript nem rolagem horizontal nos layouts móveis verificados.

As imagens do mapa foram simuladas no teste automatizado para evitar tráfego robótico aos servidores de mapas. O clique, a biblioteca local e a persistência das coordenadas foram exercitados. A disponibilidade do serviço externo e a permissão de geolocalização dependem do navegador e da conexão do usuário.

**Migração legada simulada:** V1 → V2 → V3, com proprietário sem documento, quadra, disponibilidade e duas reservas adjacentes. Todas preservadas. Ensaio dentro de uma transação descartada no banco isolado; o banco AgendaPlay da porta 5432 não foi utilizado nos testes.

Evidências: [pasta de resultados](evidencias/sprint3/), além dos relatórios completos em `target/surefire-reports` e saídas locais em `tmp/sprint3-*.log`.

## Como executar

1. Execute `iniciar.cmd` para compilar e iniciar o aplicativo, ou `executar-jar.cmd` para usar o JAR atualizado em `target`.
2. As migrações V2 e V3 são aplicadas pelo Flyway. Não recrie o banco.
3. Execute `testar.cmd` para a suíte automatizada com banco isolado.
4. O roteiro `scripts/verificar_sprint3.cjs` exige uma instância descartável do aplicativo na porta 8081 e Playwright com Edge. Não execute contra dados reais. Defina `PLAYWRIGHT_MODULE` se a biblioteca estiver fora do caminho padrão.

## Referências

- [Receita Federal — cálculo do DV de CNPJ alfanumérico](https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj).
- [Leaflet 1.9.4 — distribuição oficial](https://leafletjs.com/download.html). Biblioteca e licença BSD incluídas localmente.
- [OpenStreetMap — política de uso das imagens do mapa](https://operations.osmfoundation.org/policies/tiles/).
