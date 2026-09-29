# Conferência dos requisitos do documento da Sprint 4

Referência: **Relatório de Modelagem e Planejamento - Sprint4.docx**, recebido em 28/09/2026. Foram lidos os requisitos, as regras de negócio e os três diagramas do arquivo. Esta revisão complementa e substitui a regra temporal descrita na primeira validação.

## Problema das reservas e da baixa

A agenda do proprietário abria filtrada pelo dia atual. Uma reserva para outro dia aparecia na lista geral de Reservas, mas não nessa primeira visão da Agenda. Agora a Agenda abre com reservas de **todos os dias**, permite filtrar uma data opcionalmente e mostra a data e o número de cada reserva. O atalho Hoje aplica explicitamente a data atual; Limpar filtros retorna à visão completa. A tela de horários da quadra também ganhou acesso direto às reservas e baixas.

A versão anterior só permitia concluir depois do fim do horário. O requisito 7 e UC19 descrevem a confirmação do comparecimento. A baixa agora fica disponível **a partir do início da reserva**, após o proprietário confirmar a presença. Reservas futuras aparecem com **Dar baixa** desabilitado e uma explicação. A confirmação muda o estado para CONCLUIDA e libera a avaliação. A reserva concluída continua ocupando todo o intervalo contratado e a tolerância, para impedir uma segunda reserva conflitante.

O domínio especificado contém CLIENTE e PROPRIETARIO, sem um administrador global. Cada proprietário gerencia suas quadras. O diagnóstico do banco de uso foi somente leitura: não alterou contas, datas, reservas ou avaliações reais.

## Conferência dos 11 requisitos

| Nº | Requisito | Implementação e verificação |
| --- | --- | --- |
| 1 | Critérios da senha em linhas separadas | Lista de critérios pendentes, atualizada ao digitar. Quando todos são atendidos, o texto fica verde. |
| 2 | Preservar senha em erro de cadastro | O formulário mantém senha e confirmação em memória na tela ao receber erros, inclusive CPF inválido; não grava as senhas no armazenamento do navegador. Também aplicado ao login e redefinição. |
| 3 | Recuperar senha | Link por e-mail com token aleatório, hash no banco, validade de 30 minutos, uso único e controle de reenvio. SMTP real precisa ser configurado; envio e redefinição são testados com capturador local. |
| 4 | Ícone de olho para ver senha | Ícone de olho adicionado ao botão Mostrar/Ocultar no cadastro, login e redefinição, com nome acessível e estado pressionado. |
| 5 | Fotos da quadra | Upload JPEG/PNG, capa, galeria, troca de capa e exclusão pelo proprietário. Verificação de conteúdo e limites; imagens visíveis ao cliente. |
| 6 | Calendário | Campo de data com botão Calendário; filtro por dia na consulta dos horários e preenchimento da data da reserva. |
| 7 | Baixa pelo proprietário | Botão Dar baixa na Agenda e em Reservas, confirmação visual, disponibilidade a partir do início e validação de propriedade também no servidor. |
| 8 | Avaliar depois da baixa | Antes da baixa, a tela informa que a avaliação está aguardando o proprietário. Depois, o cliente da reserva recebe formulário de nota 1–5 e comentário opcional. Uma avaliação por reserva. |
| 9 | Navbar fixa durante rolagem | Cabeçalho sticky no topo, verificado com rolagem e layout móvel. |
| 10 | Cancelamento sem alerta nativo | Confirmação em dialog do site, com Voltar, Confirmar, Escape e restauração de foco. |
| 11 | Slider inicial | Fotos de quadras ativas, navegação, avanço automático, pausa e respeito à preferência de movimento reduzido. |

A logo com transparência permanece no cabeçalho, rodapé e favicon, atendendo também ao pedido anterior.

## Correspondência com a modelagem

As relações funcionais do documento estão preservadas: fotos pertencem a uma quadra; avaliação pertence a uma reserva e tem id_reserva único; recuperação está ligada ao usuário; CONCLUIDA libera a avaliação. Há diferenças físicas em relação ao desenho: o token é armazenado como hash na tabela `recuperacao_senha` e eliminado após uso, fotos são armazenadas em `foto_quadra.conteudo` e servidas por uma URL interna, e `concluida_em` registra a data da baixa. São decisões de implementação que conservam as regras dos 11 requisitos. Não foram renomeadas tabelas nem recriadas estruturas de produção para copiar a nomenclatura do diagrama.

## Testes e evidências

A suíte de integração passa a ter **60 testes**, incluindo o limite exato entre o instante anterior e o início da reserva, a liberação da avaliação sem esperar o fim, a agenda sem filtro e o isolamento entre proprietários. O resultado está em `docs/evidencias/sprint4/integracao.txt`.

O roteiro de navegador das Sprints 3 e 4 cobre cadastro, sessão, estabelecimentos, mapa, agenda, preços, reservas, cancelamentos, notificações, fotos, senha e recuperação. O novo cenário cria a reserva pela interface do cliente, sai da conta, entra como proprietário e verifica a reserva futura na agenda. Somente no schema isolado de teste, altera o horário desse registro para simular o comparecimento; confirma a baixa pela interface e retorna à conta do cliente para avaliar. Não usa inserção direta de uma reserva concluída como substituto desse fluxo.

Resultados de navegador: `docs/evidencias/sprint3/navegador-revalidado.json` e `docs/evidencias/sprint4/navegador.json`. Capturas ficam na mesma pasta, incluindo `reserva-futura-proprietario.png`. A aplicação de teste usa 8084 e o banco isolado usa 55432; o banco de uso em 5432 não recebe dados dos testes.

## Como utilizar

1. Entre como cliente, agende e confira o registro em Reservas.
2. Entre como proprietário da quadra e abra Agenda. A reserva aparece mesmo quando for de outro dia. Remova filtros antigos, se houver.
3. Após o início do horário e o comparecimento, clique em Dar baixa e confirme.
4. Entre novamente como o cliente que reservou, abra Reservas e envie a avaliação.
5. Para recuperação por e-mail real, configure SMTP e APP_URL conforme `docs/06_sprint4.md`. O teste local de e-mail não substitui a configuração do provedor.
