# Sprint 4 — AgendaPlay

## Funcionalidades

- Requisitos de senha exibidos individualmente, com atualização durante a digitação.
- Cadastro, login e redefinição mantêm a senha no formulário quando ocorre erro. As senhas não são gravadas em armazenamento do navegador; recarregar a página descarta os valores.
- Mostrar/ocultar senha no login, cadastro e redefinição, com ícone de olho e controles acessíveis.
- Recuperação por e-mail com token aleatório de 256 bits, armazenado somente como hash, válido por 30 minutos e de uso único. Reenvios para a mesma conta são limitados a um por minuto. A resposta pública não revela se a conta existe.
- Até 8 fotos JPEG/PNG por quadra, 5 MB por arquivo e 20 megapixels. O servidor verifica e recodifica a imagem; a capa pode ser trocada e fotos podem ser excluídas pelo proprietário.
- Capa nos cartões, galeria na consulta de horários e slider automático na página inicial. O slider tem navegação, pausa e respeita a preferência de movimento reduzido. Só divulga fotos de quadras ativas.
- Botão de calendário ao lado das datas e filtro de dia na coluna esquerda dos horários, conforme a referência recebida. Escolher uma data filtra a lista e preenche a data da reserva; “Mostrar todos os dias” remove o filtro.
- Proprietário dá baixa em uma reserva confirmada após o comparecimento, a partir do início do horário. Não é necessário esperar o fim. A conclusão não permite cancelamento posterior e preserva o intervalo reservado e o descanso.
- A agenda do proprietário abre com reservas de todos os dias; o filtro de data é opcional. Reservas futuras aparecem com o botão de baixa desabilitado e a explicação de quando ele será liberado.
- O cliente da reserva concluída pode avaliá-la uma única vez, de 1 a 5 estrelas, com comentário opcional de até 1.000 caracteres. As avaliações aparecem na quadra, com texto escapado.
- Cabeçalho acompanha a rolagem no topo. Cancelamento, baixa e exclusão de fotos usam confirmação no site, com foco de teclado e saída por Escape.
- Logo com canal alfa transparente no cabeçalho, rodapé e ícone da aba.

## Banco e instalação

As migrações V4 e V5 acrescentam fotos, avaliações e recuperação de senha e ampliam os estados da reserva. Não limpam os registros anteriores. Fotos ficam no PostgreSQL e fazem parte de seu backup.

O JAR atualizado é gerado em `target/agendaplay-0.1.0.jar`. Encerre a aplicação antes de recompilar, pois o Windows pode bloquear o arquivo em uso. Execute `executar-jar.cmd` para abrir a versão compilada com a configuração local.

## Configuração do envio de e-mail

Configure as variáveis abaixo ou os equivalentes em `config/application-local.properties`. Não compartilhe senhas no repositório.

| Variável | Finalidade |
| --- | --- |
| SMTP_HOST / SMTP_PORT | Servidor e porta fornecidos pelo provedor |
| SMTP_USER / SMTP_PASSWORD | Credenciais de envio |
| SMTP_AUTH | Habilitar autenticação, normalmente true |
| SMTP_STARTTLS | Habilitar STARTTLS, conforme o provedor |
| EMAIL_REMETENTE | Endereço autorizado para envio |
| APP_URL | Endereço público da aplicação, usado nos links; em produção, HTTPS |

Sem SMTP configurado, o aplicativo não entrega os e-mails. O erro é registrado sem token nem senha, e a interface mantém a resposta genérica para não expor contas. Os testes usam um servidor SMTP local que captura mensagens e não envia e-mails para terceiros.

## Verificação

A suíte integrada usa PostgreSQL na porta 55432, schema `agendaplay_teste`, com proteção que recusa limpar outro schema ou porta. O banco AgendaPlay em 5432 não foi limpo.

Os testes de navegador usam porta HTTP 8084 e schema separado `agendaplay_browser4`. As imagens usadas nos testes são arquivos de teste, não fotos reais de quadras.

- `scripts/verificar_sprint3.cjs`: regressão de cadastros, estabelecimentos, mapa, quadras, padrões, preços, tolerância, agenda, cancelamento e notificações; atualizado para a confirmação no site.
- `scripts/verificar_sprint4.cjs`: preservação de senha, requisitos, visibilidade, fotos/capa, slider, cabeçalho, conclusão, avaliação, recuperação via SMTP local e layouts de 390 px e 1365 px.
- Resultado integrado: consultar `target/surefire-reports`.
- Evidências visuais e resultado de navegador: `docs/evidencias/sprint4/`.

## Roteiro de apresentação

1. Cadastre um proprietário e um cliente. Demonstre um CPF inválido sem perder a senha e use Mostrar/Ocultar.
2. Cadastre estabelecimento e quadra, adicione fotos e escolha uma capa.
3. Abra a página inicial para mostrar o slider; consulte a quadra para exibir galeria, datas e horários.
4. Reserve como cliente. Abra o cancelamento e use Voltar para demonstrar a confirmação dentro do site.
5. Para demonstrar baixa, use uma reserva cujo horário já começou e confirme o comparecimento. Clique em **Dar baixa** como proprietário e avalie como o cliente daquela reserva. Reservas futuras aparecem na agenda, mas ainda não permitem baixa.
6. Com SMTP configurado, solicite recuperação e use o link recebido. Teste novamente o mesmo link para demonstrar o uso único.

## Reexecução dos testes de navegador

Com Node.js, Playwright e Microsoft Edge instalados, execute na raiz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-navegador.ps1
```

Se Playwright estiver fora do projeto, use `-PlaywrightModule` com o caminho do módulo. O script localiza o PostgreSQL instalado (ou aceita `-PostgresBin`), compila o JAR, inicia a aplicação em 8084, executa a regressão da Sprint 3 e a Sprint 4, e encerra os processos que iniciou. Somente o schema `agendaplay_browser4` do banco isolado em 55432 é recriado. Não execute duas cópias simultâneas deste roteiro.

O envio real de recuperação exige configurar SMTP e a URL pública. A configuração local recebida não contém essas opções; o teste de recuperação usa SMTP capturador local, sem entregar mensagens externas.
