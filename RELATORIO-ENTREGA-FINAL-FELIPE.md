# Relatório da Entrega Final — Felipe de Toledo Souza (@iiiFTSiii)

## 1. Contexto

- Repositório: `danilosp1/pescd-system-ES2`
- Disciplina: Engenharia de Software 2 — UFSCar
- Entrega: Final
- Integrantes: Danilo, Felipe e Guilherme

Este relatório reúne as atividades realizadas por Felipe nas issues da entrega final, as revisões feitas nos PRs da equipe, as evidências de execução e as pendências externas.

## 2. Situação das issues atribuídas a Felipe

| Issue | Item da especificação | Situação |
|---|---|---|
| #9 | #H-2 — testes de unidade | Pendente: aguarda o PR #37 (Guilherme) para atualizar os testes de `SecretaryOfferService` |
| #12 | #K — relatório de cobertura | Implementada no PR #43; CI verde |
| #14 | #M — SonarQube/SonarCloud | Transferida para o Danilo por exigência de permissão administrativa |
| #16 | #I-2 — Playwright + PageObjects | Implementada no PR #42; CI verde; PR empilhado sobre a #28 |
| #19 | #N-2 — Mud2 | Implementada no PR #41; CI verde (unidade e E2E) |
| #22 | #O-2 — warning do Sonar | Pendente: depende da #14 (Danilo) |
| #25 | #P-2 — refatoração | Pendente: depende da #9 e do PR #37 |
| #28 | #Q-2 — correção de bug | Implementada no PR #40; CI verde; issue de bug #39 criada |

## 3. Detalhamento por issue

### #12 (#K) — Cobertura de código

- Adicionado `jacoco-maven-plugin` 0.8.16 ao `pom.xml`.
- Configuradas as metas `prepare-agent` e `report`.
- Relatório HTML em `target/site/jacoco/index.html` e XML em `target/site/jacoco/jacoco.xml`.
- CI atualizado para gerar o relatório e publicar o artifact `jacoco-report`.
- Documentação adicionada ao `README.md`.
- Evidência local com 60 testes de unidade executados com sucesso:
  - branch coverage parcial no momento: 37,1%;
  - o restante da meta de 80% depende das issues #9 e #10, ainda pendentes.
- PR: https://github.com/danilosp1/pescd-system-ES2/pull/43

### #14 (#M) — SonarQube/SonarCloud

- Transferida para Danilo porque somente o proprietário/admin do repositório pode importar a organização e configurar o token no SonarCloud.
- Issue atualizada com responsável Danilo e comentário de justificativa.
- A #22 foi marcada como bloqueada por essa transferência.

### #16 (#I-2) — Testes E2E com Playwright e PageObjects

- Subprojeto `/e2e` reutilizado a partir do PR #34.
- PageObjects criados:
  - `StudentOffersPage`;
  - `StudentEnrollmentDetailsPage`;
  - `WorkPlanFormPage`;
  - `DocumentationFormPage`;
  - `ReportFormPage`;
  - `SupervisorDashboardPage`;
  - `SecretaryOfferDetailsPage` (S.04, entregue na #28).
- Testes adicionados:
  - AL.01 — listagem das ofertas do aluno;
  - AL.02 — envio do plano de trabalho;
  - AL.03 — envio da documentação;
  - AL.04 — envio do relatório final;
  - PS.01 — dashboard do supervisor;
  - S.04 — encerramento de oferta (entregue na #28).
- Suíte E2E completa executada localmente: **18/18 testes aprovados**.
- Seeds dedicados adicionados para AL.02, AL.03, AL.04 e S.04, sem interferir nos dados usados pelos testes de S.03.
- PR: https://github.com/danilosp1/pescd-system-ES2/pull/42
  - Empilhado sobre a #28 até o merge do PR #40.

### #19 (#N-2) — Mud2: reprovação e reenvio do plano

- Adicionado `PLANO_REPROVADO` ao enum de status.
- Adicionado `supervisorRejectedAt` em `WorkPlan` e no DTO de detalhes.
- Criado endpoint `POST /api/professor/supervisor/offers/{offerId}/students/{studentId}/work-plan/reject`.
- `SupervisorProfessorService.rejectPlan` valida status, salva parecer, timestamp e log.
- `StudentOfferService.submitWorkPlan` permite reenvio após reprovação e limpa a decisão anterior.
- React:
  - botão "Reprovar plano";
  - exibição do parecer de reprovação;
  - botão "Reenviar plano" na tela do aluno.
- Thymeleaf, mensagens e `REQUISITOS.md` atualizados.
- Testes de unidade: 7/7 aprovados.
- CI do PR: unidade e E2E verdes.
- PR: https://github.com/danilosp1/pescd-system-ES2/pull/41

### #28 (#Q-2) — Correção do bug

- Issue específica criada: #39 — botão "Encerrar oferta" fora do status permitido e sem confirmação/instruções.
- Correção em `OfferDetailsPage.jsx`:
  - botão exibido somente em `AGUARDANDO_ENCERRAMENTO_SECRETARIO`;
  - prévia via `GET /close` com instruções;
  - confirmação explícita antes do `POST /close`;
  - envio do campo obrigatório `confirm: true`.
- Teste E2E `s04-close-offer.spec.js` criado com PageObject.
- CI do PR: unidade e E2E verdes.
- PR: https://github.com/danilosp1/pescd-system-ES2/pull/40
- Issue de bug: https://github.com/danilosp1/pescd-system-ES2/issues/39

### #9 (#H-2) — Testes de unidade (pendente)

- Aguarda o merge do PR #37, que altera a validação da importação CSV em `SecretaryOfferService`.
- Após o merge, serão adicionados testes de `OfferService`, `SecretaryOfferService` e controllers de Secretaria.

### #22 (#O-2) — Warning do Sonar (pendente)

- Bloqueada pela #14, transferida ao Danilo.
- Permanece pendente até a análise SonarCloud estar ativa no repositório da equipe.
- Correção prevista: warning `Web:InputWithoutLabelCheck` em `templates/secretary/offers/students.html`, sem alterar os wildcard imports de `ProfessorResponsavelService`.

### #25 (#P-2) — Refatoração (pendente)

- Aguarda a #9 e o PR #37.
- Refatoração prevista: extrair a lógica de matrícula/importação de alunos do `SecretaryOfferService` para um `StudentEnrollmentService`.
- Rationale: reduzir responsabilidades do serviço, melhorar coesão e testabilidade, preservando o comportamento coberto pelos testes.

## 4. Revisões realizadas

| PR | Issue | Situação |
|---|---|---|
| #33 | #8 / #H-1 | Revisado, aprovado e mergeado; #8 fechada |
| #34 | #15 / #I-1 | Revisado, aprovado e mergeado; #15 fechada |
| #35 | #18 / #N-1 | Revisado, aprovado e mergeado; #18 fechada |

## 5. Evidências de execução

- Suíte E2E completa: 18/18 testes aprovados localmente.
- Testes de unidade da Mud2: 7/7 aprovados.
- Testes de unidade da equipe após merges de #33/#35: 60/60 aprovados.
- Build do frontend: OK.
- Compilação do backend: OK.
- JaCoCo: relatório gerado em `target/site/jacoco/`.
- PR #40 (S.04): CI de unidade e E2E verdes.
- PR #41 (Mud2): CI de unidade e E2E verdes.
- PR #42 (E2E): CI de unidade e E2E verdes.
- PR #43 (JaCoCo): CI de unidade e E2E verdes.
- Issue de bug #39 criada e vinculada à #28.

## 6. Relação com a etapa final da especificação

| Item da especificação | Issue | Status |
|---|---|---|
| #H-2 — testes de unidade JUnit 5/Mockito | #9 | Pendente (PR #37) |
| #I-2 — Playwright + PageObjects | #16 | Implementado |
| #K — relatório de cobertura | #12 | Implementado |
| #L — GitHub Actions | #13 | Concluído por Danilo |
| #M — SonarQube/SonarCloud | #14 | Transferido ao Danilo |
| #N-2 — Mud2 | #19 | Implementado |
| #O-2 — warning Sonar | #22 | Pendente (#14) |
| #P-2 — refatoração | #25 | Pendente (#9/PR #37) |
| #Q-2 — correção de bug | #28 + #39 | Implementado |

## 7. Pendências e riscos

1. **#9 e #25:** dependem do PR #37 (Guilherme).
2. **#22:** depende da #14 (Danilo).
3. **#16:** empilhada sobre a #28; deve ser rebaseada/retargetada após o merge do PR #40.
4. **Merges dos PRs #40, #41, #42 e #43:** dependem da revisão de Danilo/Guilherme.
5. A API do GitHub apresentou indisponibilidade parcial durante a execução; as branches e PRs foram criados após a recuperação, com CI verde.

## 8. Conclusão

As implementações sob responsabilidade de Felipe foram concluídas para #12, #16, #19 e #28, com testes automatizados e evidências de CI. As pendências #9, #22 e #25 dependem, respectivamente, do PR #37 (Guilherme) e da configuração SonarCloud (#14, Danilo), conforme alinhado com a equipe.
