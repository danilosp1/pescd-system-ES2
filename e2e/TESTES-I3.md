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

Os comentários explicam apenas a preparação dos dados, a repetição dos testes e a falha conhecida.

## Validação local

A suíte completa executou 32 cenários em Chromium, com 31 aprovações e uma falha esperada de PS.03. Nenhum teste foi ignorado e não houve falhas inesperadas nem casos instáveis. O I-3 acrescenta 19 cenários.

## Falha conhecida

PS.03 RN-3 pede que a frequência indicada pelo aluno apareça preenchida. A interface React inicializa esse campo vazio. O cenário específico usa `test.fail` com justificativa e executa a asserção esperada de 95%. Se o problema for corrigido, o Playwright apontará um sucesso inesperado para que a marcação seja removida.

O retorno sem salvar usa o link Voltar disponível nas telas.
