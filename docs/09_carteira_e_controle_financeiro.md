# Carteira e controle financeiro

## Regras adotadas

- A carteira está disponível somente para clientes. A recarga é uma simulação acadêmica: aumenta o saldo interno e registra `RECARGA`, sem operação bancária ou entrada no caixa do proprietário.
- O cliente pode pagar o saldo restante de uma reserva confirmada. O débito cria `PAGAMENTO` na carteira e um recebimento `CARTEIRA` na mesma transação. Esse recebimento aparece no financeiro existente e no painel do proprietário uma única vez. Não exige caixa aberto, porque não altera o numerário físico.
- O cashback incide apenas sobre o valor efetivamente pago com carteira, depois do pagamento. Padrão: 5%, valor mínimo R$ 0,00, habilitado. Configure por `CASHBACK_PERCENTUAL` (0 a 100), `CASHBACK_VALOR_MINIMO` e `CASHBACK_ATIVO`. O bônus é arredondado para centavos e creditado no saldo. Não é receita do estabelecimento nem movimento de caixa.
- Ao cancelar uma reserva paga com carteira, o valor pago retorna automaticamente como `ESTORNO`, o cashback daquela reserva é debitado como `REVERSAO_CASHBACK`, e o recebimento fica marcado como estornado. Valores recebidos por formas manuais continuam aguardando confirmação do estorno manual pelo proprietário. O prazo de cancelamento existente continua valendo.
- O banco impede saldo negativo, duplicação de pagamento por reserva, duplicação de cashback e repetição da chave de recarga. As operações bloqueiam as linhas da reserva e da carteira em transações; reenvios idênticos são idempotentes.
- O painel do proprietário soma receita contratada das reservas não canceladas, valores recebidos ainda não estornados, despesas cadastradas e resultado (`recebido - despesas`). A receita e os recebimentos usam a data da reserva; as despesas usam a data informada. Filtros de período são inclusivos e só mostram estabelecimentos do proprietário autenticado. Recebimentos da carteira são contabilizados na tabela de recebimentos já existente, sem outra soma da carteira.
- Despesas são lançamentos administrativos por estabelecimento. O registro não reduz automaticamente o saldo de caixa, pois pode representar Pix, cartão ou outra forma não informada. O controle de caixa físico permanece restrito aos recebimentos manuais em dinheiro e estornos manuais registrados nele.

## Evolução e execução

`V9__carteira_e_despesas.sql` acrescenta as tabelas de saldo, movimentos e despesas, e permite `CARTEIRA` em recebimentos. Nenhuma migração limpa ou recria os dados existentes. Execute `iniciar.cmd` na raiz para compilar e iniciar com a configuração local e aplicar a migração via Flyway. O banco principal não foi usado durante os testes automatizados; `testar.cmd` usa a instância isolada na porta 55432.
