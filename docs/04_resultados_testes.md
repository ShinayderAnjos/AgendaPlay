# Resultados da validação técnica

Data: 13/09/2026. Versão: 0.1.0.

## Testes integrados

**21 testes executados; 0 falhas; 0 erros; 0 ignorados.**

Execuções realizadas com Java 21 e Java 25, Spring Boot 3.5.16 e PostgreSQL 17.6. A estrutura de teste fica na instância local isolada da porta 55432, schema `agendaplay_teste`. Os dados foram criados automaticamente para cada caso e não representam usuários reais.

Cobertura exercitada:

- Renderização das páginas públicas e autenticadas com dados.
- Cadastro, validação, normalização do e-mail, duplicidade, confirmação de senha e hash BCrypt.
- Cadastro de proprietário com CPF opcional; perfil atribuído no servidor.
- Login inválido/válido, logout e invalidação da sessão.
- CSRF, isolamento de perfis e proteção contra alteração de IDs de proprietário/cliente.
- Cadastro e edição de quadra, rejeição de valor negativo e acesso de outro proprietário.
- Catálogo sem informações privadas.
- Datas passadas, duração inválida e disponibilidades duplicadas/sobrepostas.
- Cálculo proporcional do preço e recorte dos intervalos livres.
- Sobreposição parcial/total e reserva fora da disponibilidade.
- Restrições de banco para papéis e conflitos, inclusive inserção direta pelo repositório.
- Duas solicitações simultâneas: uma confirmação e uma rejeição.
- Quadra inativa, preservação de histórico, consulta por usuário e filtros.
- Cancelamento autorizado, liberação de horário e bloqueio de inativação com reservas.
- Criação de reserva pelo formulário e erro de conflito compreensível.

O relatório original da última execução pode ser reproduzido com `testar.cmd` e consultado em `target/surefire-reports`.

## Teste real de navegador

Executado no Microsoft Edge, em contexto isolado, com dados fictícios:

1. Página inicial e login do proprietário.
2. Listagem das quadras e abertura da agenda.
3. Logout e login do cliente.
4. Seleção de horário, confirmação da reserva e consulta da confirmação.
5. Cancelamento com confirmação e conferência da mensagem de sucesso.
6. Página inicial e cadastro em 390 px de largura, sem rolagem horizontal.

Resultado: aprovado, sem erros de JavaScript capturados. Capturas inspecionadas em `docs/evidencias/`.

## Limites desta evidência

Os testes acima validam a implementação em ambiente isolado. Não constituem aceite de Geanderson/Lázaro, homologação dos diagramas por Diego ou teste de carga em produção.

A política de antecedência do cancelamento é provisória e requer definição com os usuários.

## Conexão do banco principal

Concluída em 13/09/2026 após a atualização informada pelo usuário. Aplicação disponível em http://localhost:8080, conectada ao PostgreSQL 17.6, banco `AgendaPlay`, porta 5432.

Conferência direta por IPv4 (127.0.0.1): tabelas `usuario`, `quadra`, `disponibilidade`, `reserva` e `flyway_schema_history` presentes no schema `agendaplay`; migração V1 registrada com sucesso. O Spring Boot iniciou normalmente e validou a estrutura existente.

Verificação adicional no navegador: página inicial, login e cadastros responderam normalmente; tentativa de login com usuário inexistente retornou mensagem genérica; rota de reservas exigiu autenticação. Nenhum cadastro foi criado nessa verificação do banco principal.

O endereço `localhost` apresentou diferença entre IPv4 e IPv6 na conexão direta pelo psql nesta máquina: IPv4 foi validado. A aplicação iniciou e conectou normalmente.

A configuração privada continua fora do versionamento e do pacote para a equipe. Nenhuma senha foi incluída na entrega.
