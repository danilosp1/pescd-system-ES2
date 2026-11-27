# Testes web de Professores

O I-3 cobre as seis histórias de Professor atribuídas a Guilherme na issue #17.

| História | Arquivo | Cenários |
| --- | --- | --- |
| PS.02 | ps02-approve-plan.spec.js | Dados e PDF do plano, aprovação persistida, parecer obrigatório e retorno sem salvar. |
| PS.03 | ps03-approve-report.spec.js | Dados do relatório, parecer, frequência, sugestão de nota, validação, retorno e frequência pré-preenchida. |
| PR.01 | pr01-conclude-report.spec.js | Avaliação do supervisor, conclusão persistida, campos obrigatórios, frequência inválida e retorno. |
| PR.02 | pr02-analyze-documentation.spec.js | Documentação, análise persistida, nota obrigatória e retorno. |
| PR.03 | pr03-request-closure.spec.js | Resumo, estatísticas, lições aprendidas, encaminhamento ao secretário, retorno e alunos pendentes. |
| PR.04 | pr04-track-offers.spec.js | Oferta, alunos, histórico, filtro por nome, busca vazia, ausência de resultados e acesso de outro professor. |

## Execução

Com o backend na porta 8082 e o frontend na porta 5173, execute dentro de e2e.

```sh
npm ci
npx playwright install chromium
npx playwright test tests/ps02-approve-plan.spec.js tests/ps03-approve-report.spec.js tests/pr01-conclude-report.spec.js tests/pr02-analyze-documentation.spec.js tests/pr03-request-closure.spec.js tests/pr04-track-offers.spec.js
```

Para executar toda a suíte, use `npm test`. Para abrir o relatório HTML, use `npm run report`.

## Padrão e dados

Os arquivos seguem os testes existentes da equipe, com describe por história, Page Objects em pages, LoginPage compartilhado e verificações Playwright. SupervisorDashboardPage e samplePdf seguem os nomes e o formato do I-2 para facilitar a integração.

A fixture professorScenario prepara ofertas e alunos exclusivos pela API real. A ação principal de cada cenário ocorre no navegador, com verificações de status, histórico e persistência. Cada execução usa identificadores únicos e pode repetir o I-3 sem resetar o banco.

Os comentários explicam a preparação dos dados e a repetição dos testes.

## Validação local

O I-3 acrescentou 19 cenários. A suíte completa passou com 32 cenários em Chromium após o aprimoramento #54.

## Frequência na aprovação do relatório

O teste de PS.03 RN-3 verifica que o campo inicia com os 95% enviados pelo aluno e que a aprovação grava esse valor quando o supervisor o confirma sem editar. Outros cenários verificam a edição do valor e a validação de campo obrigatório.

O retorno sem salvar usa o link Voltar disponível nas telas.
