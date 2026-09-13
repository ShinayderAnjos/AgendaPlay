# Arquitetura e conferência da modelagem

## Caminho de uma requisição

1. O navegador envia o formulário, acompanhado do token CSRF.
2. O Spring Security verifica a sessão e o perfil autorizado para a rota.
3. O controller valida o formato dos dados do formulário e chama o service.
4. O service verifica as regras do produto e inicia uma transação quando há alteração de dados.
5. O repository executa SQL parametrizado no PostgreSQL.
6. O controller apresenta a tela ou redireciona após o sucesso. Erros de validação reapresentam o formulário com mensagens em português.

O frontend está dentro do mesmo projeto Spring Boot. Isso permite executar toda a aplicação com um único processo Java e evita configuração de CORS e sincronização de dois servidores nesta entrega.

## Modelo implementado

| Tabela | Responsabilidade | Relações |
| --- | --- | --- |
| usuario | Nome, e-mail, CPF, telefone, senha_hash, perfil e criado_em | Uma tabela física para Cliente e Proprietario |
| quadra | Nome, modalidade, localização, valor_hora, situação e criado_em | Pertence a um usuário de perfil PROPRIETARIO |
| disponibilidade | Data, hora_inicio, hora_fim, ativo e criado_em | Pertence a uma quadra |
| reserva | Data, hora_inicio, hora_fim, situação, valor_total e criado_em | Pertence a uma quadra e a um usuário de perfil CLIENTE |

Todas as PKs são `BIGINT GENERATED ALWAYS AS IDENTITY`. Valores monetários usam `NUMERIC` no banco e `BigDecimal` no Java. As datas/horas dos agendamentos usam `LocalDate`/`LocalTime`; o relógio da aplicação usa `America/Sao_Paulo`. A auditoria usa `TIMESTAMPTZ`.

`Cliente` e `Proprietario` herdam de `Usuario`. O repositório instancia a subclasse de acordo com `perfil`, mantendo a tabela única pedida pelo documento. As demais entidades são records imutáveis; as mudanças são realizadas por services dentro das transações.

## Segurança e integridade

- O papel vem da rota validada de cadastro, e não de um campo oculto informado pelo navegador.
- E-mails são normalizados para minúsculas e protegidos por unicidade no banco. CPF é único quando informado e obrigatório para CLIENTE.
- BCrypt com custo 12; senha nunca é salva em texto simples. A confirmação é verificada no servidor. Limite adicional de 72 bytes respeita o algoritmo.
- Sessão expira após 30 minutos de inatividade; logout invalida a sessão e apaga o cookie.
- CSRF permanece habilitado inclusive em login/logout; cookies HttpOnly e SameSite=Lax.
- A autorização é aplicada nas rotas e nas regras de propriedade. IDs enviados pelo formulário não podem mudar o cliente da reserva ou o proprietário da quadra.
- SQL parametrizado; páginas escapam os valores com Thymeleaf. Dados privados não aparecem no catálogo.
- As FKs compostas com `perfil_proprietario` e `perfil_cliente` garantem os papéis também no banco.

## Reservas simultâneas

Todas as alterações de agenda bloqueiam primeiro a linha da quadra com `SELECT ... FOR UPDATE`. Dentro da mesma transação, a aplicação consulta a situação atual e os horários livres, valida e grava. Uma segunda solicitação aguarda e refaz as verificações.

A restrição de exclusão GiST no PostgreSQL é uma segunda garantia: duas reservas CONFIRMADAS não podem ter intervalos sobrepostos na mesma quadra, mesmo que alguém tente inserir diretamente pela camada de persistência. A mesma regra impede disponibilidades ativas sobrepostas.

O intervalo é `[início, fim)`: 10h–11h e 11h–12h podem coexistir. Não há encaixe se uma reserva ultrapassar a disponibilidade. Intervalos livres são calculados subtraindo as reservas confirmadas, sem oferecer partes já ocupadas.

Inativar uma quadra preserva reservas existentes. Inativar uma disponibilidade com reservas confirmadas é bloqueado. Cancelar muda o status da reserva, mantém seu valor e histórico e libera o intervalo quando a quadra e a disponibilidade continuam válidas.

## Conferência com Diego

A fonte utilizada foi `Sprint1.pdf`, incluindo o DER e o diagrama de classes, mais as histórias de `Scrum Aplicado à Disciplina - AgendaPlay 24_08 (1).pdf`.

| Ponto | Implementação / decisão a conferir |
| --- | --- |
| Tabela única USUARIO | Mantida; perfil CLIENTE ou PROPRIETARIO |
| CPF do cliente | Obrigatório no cliente; opcional no proprietário, conforme nota do DER |
| Nomes das chaves | PK física abreviada para `id`; FKs continuam `id_cliente`, `id_proprietario`, `id_quadra` |
| Identificadores INT | Ampliados para BIGINT; mesmas cardinalidades |
| Senha VARCHAR no DER | `senha_hash` BCrypt; não há atributo com senha bruta persistida |
| Valor `Double` nas classes | `BigDecimal` para evitar arredondamento binário de moeda |
| Métodos nas entidades | Regras de negócio e transações concentradas nos services; entidades de agenda imutáveis |
| Auditoria criado_em | Persistida nas quatro tabelas; não exposta nos formulários desta versão |
| Restrições de perfil | Implementadas no serviço e reforçadas por FKs compostas; colunas auxiliares não aparecem nas telas |
| Situações | Quadra ATIVA/INATIVA; reserva CONFIRMADA/CANCELADA; disponibilidade ativo true/false |
| Cancelamento | Parâmetro de antecedência em minutos; provisoriamente 0 e sempre antes do início |

Essas diferenças de implementação precisam ser refletidas ou aceitas na revisão dos diagramas. Não foi presumida aprovação do Diego.

## Pontos para decisão do produto

- Validar a antecedência mínima de cancelamento com o Product Owner e usuários antes da versão final.
- Confirmar se intervalos arbitrários por minuto e valor proporcional atendem aos centros esportivos, ou se haverá blocos fixos.
- Confirmar se haverá regras específicas para disponibilidade que cruza a meia-noite. Nesta versão, cada intervalo fica no mesmo dia; use dois dias separados quando necessário.
- Em implantação pública futura, configurar HTTPS, cookie Secure e proteção contra tentativas repetidas de login. A versão atual é destinada a testes locais/homologação controlada.
