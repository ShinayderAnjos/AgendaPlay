# Evolução do financeiro — Sprint 2

O proprietário também administra o caixa. Cada estabelecimento tem seu próprio caixa e cada cliente consulta apenas as suas reservas. O cadastro dos perfis existentes foi preservado.

## Como testar

1. Feche o servidor anterior com Ctrl+C e execute `iniciar.cmd` na pasta da aplicação.
2. Entre como proprietário e abra **Caixa**. Escolha o estabelecimento e informe o dinheiro disponível na abertura (pode ser zero).
3. Em **Financeiro**, encontre uma reserva. Informe o valor recebido e a forma de recebimento. O botão **Usar 50% do valor total** preenche uma entrada opcional; qualquer parcela positiva até o saldo restante é aceita.
4. Registre as próximas parcelas, inclusive com formas diferentes. Confira o histórico, o total registrado e o saldo restante. A reserva só fica quitada ao atingir o valor total.
5. Entre na conta cliente e abra **Meu financeiro**. Confira seus pagamentos, valores restantes e estornos. O cliente não registra recebimentos.
6. Em **Caixa**, consulte as entradas e os estornos por forma. No fechamento, informe o dinheiro contado e uma observação opcional. A diferença fica salva.
7. Para próximos recebimentos ou estornos, abra outro caixa. O histórico dos caixas fechados permanece disponível.

**Exemplo:** reserva de R$ 120,00; parcelas de R$ 60,00 em Pix, R$ 30,00 em dinheiro e R$ 30,00 no cartão. Saldo restante: zero. Com abertura de R$ 100,00, o dinheiro esperado no fechamento é R$ 130,00.

Um caixa aberto é necessário para novos recebimentos e estornos. Pix, cartão e transferência aparecem no resumo, mas não aumentam o dinheiro físico. Estornos são registrados nas formas de recebimento originais, após a devolução efetiva ao cliente. Se apenas uma parte da reserva tiver sido paga, somente essa parte será estornada. Não há integração bancária ou cobrança automática.

O mês no financeiro é o mês da reserva; a apuração do caixa considera quando as operações foram registradas. Valores pagos em reservas canceladas permanecem no líquido até o estorno ser registrado.

## Recuperação de senha por e-mail

1. Em **Entrar**, selecione **Esqueci minha senha** e solicite o código.
2. Digite o e-mail e o código de seis dígitos recebido.
3. Após validar, cadastre e confirme uma senha que atenda aos requisitos existentes.
4. Entre novamente usando a nova senha.

O código expira em 10 minutos, admite até cinco tentativas e funciona uma única vez. Um novo envio invalida a recuperação anterior. Há intervalo mínimo de um minuto e limite de cinco envios por hora por conta. O código é armazenado como hash; após validá-lo, uma autorização temporária fica na sessão para a troca de senha. Links emitidos antes da atualização continuam funcionando até sua expiração.

O SMTP precisa estar configurado. Use as configurações já existentes em `config/application-local.properties` ou as variáveis `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `EMAIL_REMETENTE` e `APP_URL`. Não envie senhas SMTP para o GitHub. Sem um serviço SMTP configurado, o fluxo não entrega e-mails. Os testes automatizados usam e-mails de teste e captura SMTP local.

## Persistência e validação

As migrações V7 e V8 são aplicadas pelo Flyway na próxima inicialização. V7 converte cada recebimento antigo em uma parcela com o valor integral da reserva, mantendo forma, datas e estornos. Registros antigos aparecem identificados como anteriores ao controle de caixa, sem atribuição artificial a um caixa novo.

As reservas e seus preços originais são preservados. Validações impedem parcelas nulas, negativas, com mais de duas casas decimais ou acima do saldo. Reenvios da mesma operação não duplicam o recebimento. Bloqueios transacionais protegem recebimentos concorrentes e fechamento de caixa.

Execute `testar.cmd` para os testes de integração em PostgreSQL isolado na porta 55432. `scripts/testar-navegador.ps1` revalida os fluxos anteriores e o financeiro, com capturas de tela em `docs/evidencias/financeiro-sprint2`.

Referência para a recuperação por código: [OWASP — Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
