import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SupervisorDashboardPage } from '../pages/SupervisorDashboardPage.js';
import { SupervisorEvaluationPage } from '../pages/SupervisorEvaluationPage.js';
import { createProfessorScenario } from '../fixtures/professorScenario.js';

test.describe('PS.03 - Aprovação do relatório pelo supervisor', () => {
  let scenario;
  let dashboard;
  let evaluation;

  test.beforeEach(async ({ page, request }) => {
    scenario = await createProfessorScenario(request, 'PS03', 'report');
    const login = new LoginPage(page);
    dashboard = new SupervisorDashboardPage(page);
    evaluation = new SupervisorEvaluationPage(page);
    await login.goto();
    await login.login('alevada', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await dashboard.openStudent(scenario.offerName, scenario.student.fullName, 'Aprovar relatório');
  });

  test('deve apresentar o plano, o relatório e salvar frequência, sugestão de nota e parecer', async ({ page }) => {
    await evaluation.expectPlanDetails();
    await expect(page.locator('.card').filter({ hasText: 'Relatório final (enviado pelo aluno)' })).toContainText('95%');
    await expect(page.getByRole('button', { name: /Visualizar PDF do Relatório/ })).toBeVisible();
    await evaluation.approveReport({ parecer: 'Relatório aprovado pelo E2E', frequency: 90, grade: 'B' });
    await evaluation.expectStatus('Relatório aprovado pelo supervisor');
    await page.reload();
    await expect(evaluation.status).toHaveText('Relatório aprovado pelo supervisor');
    await expect(evaluation.reportForm).toHaveCount(0);
    const response = await page.request.get(`/api/professor/supervisor/offers/${scenario.offerId}/students/${scenario.student.studentId}/report`);
    await expect(response).toBeOK();
    const report = await response.json();
    expect(report.supervisorParecer).toBe('Relatório aprovado pelo E2E');
    expect(report.supervisorFrequencia).toBe(90);
    expect(report.supervisorNotaSugestao).toBe('B');
    expect(report.supervisorApprovedAt).toBeTruthy();
  });

  test('deve exigir parecer, nota e frequência entre 0 e 100', async () => {
    await evaluation.reportForm.getByRole('button').click();
    expect(await evaluation.reportForm.getByRole('textbox').evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    await evaluation.reportForm.getByRole('textbox').fill('Parecer preenchido');
    await evaluation.reportForm.getByRole('spinbutton').fill('95');
    await evaluation.reportForm.getByRole('button').click();
    expect(await evaluation.reportForm.getByRole('combobox').evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    const frequency = evaluation.reportForm.getByRole('spinbutton');
    await evaluation.reportForm.getByRole('combobox').selectOption('A');
    await frequency.clear();
    await evaluation.reportForm.getByRole('button').click();
    expect(await frequency.evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    await frequency.fill('101');
    await evaluation.reportForm.getByRole('button').click();
    expect(await frequency.evaluate((input) => input.validity.rangeOverflow)).toBeTruthy();
    await expect(evaluation.status).toHaveText('Relatório enviado');
  });

  test('deve voltar sem alterar o relatório enviado', async () => {
    await evaluation.reportForm.getByRole('textbox').fill('Rascunho');
    await evaluation.goBack();
    await dashboard.expectStudentStatus(scenario.offerName, scenario.student.fullName, 'Relatório enviado');
  });

  test('deve pré-preencher e salvar a frequência do aluno sem exigir edição', async ({ page }) => {
    await expect(evaluation.reportForm.getByRole('spinbutton')).toHaveValue('95');
    await evaluation.reportForm.getByRole('textbox').fill('Frequência do aluno confirmada');
    await evaluation.reportForm.getByRole('combobox').selectOption('A');
    await evaluation.reportForm.getByRole('button').click();
    await evaluation.expectStatus('Relatório aprovado pelo supervisor');

    const response = await page.request.get(`/api/professor/supervisor/offers/${scenario.offerId}/students/${scenario.student.studentId}/report`);
    await expect(response).toBeOK();
    expect(await response.json()).toMatchObject({ supervisorFrequencia: 95 });
  });
});
