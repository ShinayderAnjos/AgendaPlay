# AgendaPlay

Plataforma web de gestão e agendamento de quadras esportivas. Versão 0.1.0 para testes da equipe.

## Comece por aqui

1. Abra esta pasta no VS Code. O `pom.xml` fica na raiz e identifica o projeto Maven.
2. Tenha **Java 21 ou superior** e **PostgreSQL 17** instalados. O Maven Wrapper baixa a versão necessária do Maven na primeira compilação.
3. No pgAdmin, confirme a existência do banco **AgendaPlay**. Não recrie o banco existente.
4. Copie `config/application-local.properties.example` para `config/application-local.properties`, caso ainda não exista. Preencha usuário e senha locais. O arquivo preenchido não deve ser enviado à equipe nem incluído no Git.
5. No Windows, execute `iniciar.cmd`. Ele compila o sistema e inicia o servidor. A primeira execução precisa de internet para baixar as dependências.
6. Se recebeu o pacote com JAR, `executar-jar.cmd` inicia sem recompilar. Abra **http://localhost:8080**. Mantenha a janela do servidor aberta enquanto utiliza o sistema. Use Ctrl+C nessa janela para encerrar.

A senha do PostgreSQL é diferente da senha das contas de cliente/proprietário criadas na plataforma.

O sistema aplica automaticamente a migração SQL pelo Flyway. No pgAdmin, atualize a árvore: **AgendaPlay → Schemas → agendaplay → Tables**. As quatro tabelas do domínio e o histórico de migrações aparecerão ali.

## Primeiro teste em cinco passos

1. Cadastre uma conta de **proprietário** pela página inicial e entre.
2. Cadastre uma quadra ativa, com modalidade, endereço e valor por hora.
3. Abra **Disponibilidades** e informe uma data futura, por exemplo das 08:00 às 18:00.
4. Saia e crie uma conta de **cliente**, com CPF válido para validação. Em ambiente de teste, use dados fictícios do roteiro.
5. Escolha a quadra, selecione um horário e confirme a reserva. Consulte **Reservas**. Ao entrar como proprietário, a mesma reserva aparecerá na agenda da quadra.

## Organização da pasta

```text
Projeto_14/
├── pom.xml                         Dependências e configuração Maven
├── mvnw / mvnw.cmd                 Maven Wrapper
├── iniciar.cmd                    Inicialização no Windows
├── testar.cmd                     Testes com PostgreSQL separado
├── config/                        Conexão local e modelo sem senha
├── banco/                         Comandos SQL explicados em .txt
├── docs/                          Arquitetura, roteiro e evidências de testes
├── scripts/                       Inicialização e testes
├── src/main/java/br/com/agendaplay/
│   ├── config/                    Configuração de segurança
│   ├── controller/                Recebe requisições e escolhe as telas
│   ├── dto/                       Dados dos formulários e validações
│   ├── model/                     Usuario, Cliente, Proprietario e demais entidades
│   ├── repository/                Consultas SQL e persistência
│   ├── service/                   Regras de negócio e transações
│   ├── security/                  Usuário autenticado na sessão
│   └── exception/                 Erros de negócio compreensíveis
├── src/main/resources/
│   ├── db/migration/              Migrações versionadas do banco
│   ├── templates/                 Páginas Thymeleaf
│   └── static/                    CSS e JavaScript
├── src/test/                      Testes integrados
└── target/                        Resultado da compilação (gerado)
```

Os PDFs recebidos continuam preservados na raiz. `tmp/` contém arquivos temporários de desenvolvimento e não faz parte da entrega.

## Tecnologias e decisões

- Java 21, Spring Boot 3.5.16 e Maven 3.9.16.
- Spring MVC e Thymeleaf para telas integradas ao backend, sem exigir um segundo servidor de frontend.
- Spring Security, sessão HTTP, proteção CSRF e BCrypt para as senhas.
- Spring JDBC (`JdbcClient`) para manter o SQL visível nos repositórios; não utiliza JPA/Hibernate para persistência.
- PostgreSQL 17 e Flyway para controle da estrutura do banco.
- HTML, CSS e JavaScript locais, sem fontes ou serviços externos necessários ao uso das telas.

Compatibilidade Java/Spring: https://docs.spring.io/spring-boot/3.5/system-requirements.html

## Funcionalidades

| Entrega | Funcionalidades |
| --- | --- |
| Base da Sprint 1 | Arquitetura em camadas; banco versionado; cadastro de clientes e proprietários; login/logout; separação dos perfis; cadastro e consulta de quadras; frontend integrado |
| Fluxo central da Sprint 2 | Edição/inativação de quadras; disponibilidades; cálculo de horários livres; reservas; bloqueio de conflitos; consulta por cliente; agenda do proprietário com filtros |
| Regra provisória | Cancelamento antes do início, sem antecedência mínima adicional; depende de validação do Product Owner e usuários |

A consulta de contas é feita em **Minha conta**. O proprietário vê somente o nome do cliente associado às reservas de suas próprias quadras. Não foi criada uma listagem pública de pessoas nem um perfil de administrador, pois isso não está previsto nas histórias fornecidas.

## Testes

Execute `testar.cmd`. O roteiro inicia um PostgreSQL isolado na porta **55432**, dentro de `tmp/postgres-test`, executa os testes e encerra a instância que iniciou. A estrutura `agendaplay_teste` usa exclusivamente dados fictícios e é reiniciada a cada caso. **O banco AgendaPlay da porta 5432 não é limpo por esses testes.**

O relatório Maven fica em `target/surefire-reports`. A evidência da validação inicial está em `docs/04_resultados_testes.md`. Para testes manuais e registro de defeitos, siga `docs/02_roteiro_testes.md`.

Também é possível executar `./mvnw test` em outro sistema após preparar PostgreSQL de teste com a conexão de `src/test/resources/application-test.properties`. Não aponte o perfil de testes para dados reais.

## Envio à equipe

Use o pacote de entrega preparado na pasta `entrega/`. Ele contém código, JAR executável, modelo de configuração, SQL e documentação, sem a senha local e sem os dados dos testes automáticos.

Cada tester pode executar localmente com seu PostgreSQL. `localhost` aponta para o próprio computador de quem abre o endereço. Acesso remoto compartilhado exige um servidor de homologação; esta entrega não foi publicada na internet.

## Dúvidas frequentes

- **Senha recusada pelo banco:** confira usuário/senha em `config/application-local.properties`; não coloque aspas ao redor da senha. Em arquivos `.properties`, uma barra invertida literal deve ser escrita como `\\`.
- **Porta 8080 ocupada:** encerre a outra execução do AgendaPlay ou defina `server.port=8082` na configuração local e acesse a nova porta.
- **Tabelas não aparecem no pgAdmin:** atualize o schema `agendaplay`, não somente `public`.
- **Migração recusada em tabelas criadas manualmente:** não exclua dados nem ative `clean`/`baseline` automaticamente; compare a estrutura com a migração antes de decidir a correção.
- **Extensão btree_gist recusada:** a criação inicial requer permissão para instalar a extensão no banco. O responsável pelo PostgreSQL pode executar o comando documentado em `banco/01_criacao_banco_e_tabelas.txt`.

A validação técnica automatizada não substitui a aceitação de você, Lázaro, Diego e representantes dos usuários.
