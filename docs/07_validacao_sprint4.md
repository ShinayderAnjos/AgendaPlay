# Validação da Sprint 4 — 28/09/2026

Registro da primeira revisão. A revisão posterior baseada no documento de modelagem está em [08_conferencia_documento_sprint4.md](08_conferencia_documento_sprint4.md) e substitui a regra anterior de baixa somente após o fim do horário.

## Resultado

A implementação recebida em `Downloads/Projeto_28` já continha as funcionalidades principais da Sprint 4. Foi revisada em uma cópia de trabalho e complementada com preservação de senha no login inválido, descrição acessível das confirmações e um roteiro reproduzível dos testes de navegador. As imagens de referência fornecidas foram conferidas; o calendário fica na coluna dos horários, junto ao campo de data.

O chat compartilhado não pôde ser recuperado pela consulta web. A verificação foi feita sobre o código local, as imagens fornecidas e execuções novas dos testes, sem usar os resultados antigos como comprovação desta entrega.

## Testes executados

| Verificação | Resultado |
| --- | --- |
| Integração Java/Spring/PostgreSQL | 58 testes, zero falhas, zero erros, zero ignorados |
| Regressão de navegador Sprint 3 | 14 fluxos registrados, aprovados |
| Navegador Sprint 4 | 20 verificações registradas, aprovadas |
| Erros JavaScript na Sprint 4 | Zero |
| Alert/confirm/prompt nativos na Sprint 4 | Zero nos fluxos executados |
| Layouts | 1365 × 900 e 390 × 844; verificações de overflow e capturas |
| Compilação | JAR executável gerado com sucesso |
| Logo | PNG RGBA com transparência, presente no cabeçalho, rodapé e favicon |

A integração cobre cadastro, documentos, senha forte, login/logout, CSRF, isolamento de perfis e proprietários, valores, horários, conflitos e concorrência de reservas, cancelamentos, notificações, padrões semanais, fotos e limites de upload, conclusão, avaliação e recuperação de senha (expiração, uso único, concorrência e falha de envio).

No navegador foram verificados erros de CPF e de confirmação sem perda da senha; requisitos em linhas separadas; mostrar/ocultar; login com e-mail errado, seguido de correção sem redigitar a senha; fotos e troca de capa; slider manual, automático e pausa; cabeçalho durante rolagem; modal com confirmação, desistência e Escape/foco; calendário com dia disponível e dia vazio; baixa pelo proprietário; avaliação do cliente; captura de e-mail, redefinição, login com a nova senha e rejeição de token reutilizado.

As capturas usam imagens artificiais de teste, incluindo logo e screenshot, para verificar upload e exibição. Não foram adicionadas fotos artificiais ao banco de apresentação.

## Evidências e reexecução

- `target/surefire-reports/br.com.agendaplay.AgendaPlayIntegrationTest.txt`
- `docs/evidencias/sprint4/integracao.txt`
- `docs/evidencias/sprint3/navegador-revalidado.json`
- `docs/evidencias/sprint4/navegador.json`
- Capturas PNG em `docs/evidencias/sprint4/`
- Integração: `testar.cmd`
- Navegador: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-navegador.ps1`

O roteiro aceita `-PostgresBin` e `-PlaywrightModule`. Requer Node.js, Playwright e Edge. A aplicação de teste usa 8084; PostgreSQL usa 55432, com schemas `agendaplay_teste` e `agendaplay_browser4`. O roteiro de navegador recria somente seu schema isolado e encerra os processos que inicia. O banco de apresentação em 5432 não foi limpo nem usado nos testes.

Ambiente desta execução: Windows, JDK 25.0.4.1 compilando para Java 21, PostgreSQL 18.3 e Edge. Flyway emitiu aviso de que sua versão declara suporte testado até PostgreSQL 17; as cinco migrações e os testes passaram no PostgreSQL 18 local. As advertências de agentes Mockito/JVM não impediram a execução.

## Antes de apresentar

1. Reinicie a aplicação com o JAR atualizado para carregar os ajustes.
2. Cadastre fotos reais das quadras que deseja mostrar no slider.
3. Configure SMTP e a URL pública para receber recuperação em e-mail real. A configuração local recebida não contém essas opções; o teste usou um capturador SMTP local na porta 2526, sem envio externo.
4. Para demonstrar avaliação, use uma reserva cujo horário já começou e dê baixa após o comparecimento, na conta do proprietário. O cliente poderá avaliá-la uma vez.

Os testes aprovados cobrem os cenários descritos; não representam garantia de ausência de defeitos em todos os ambientes, navegadores ou provedores de e-mail.
