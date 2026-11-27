import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { SupervisorDashboardPage } from '../pages/SupervisorDashboardPage.js';
import { SupervisorEvaluationPage } from '../pages/SupervisorEvaluationPage.js';
import { createProfessorScenario } from '../fixtures/professorScenario.js';

test.describe('PS.02 - Aprovação do plano pelo supervisor', () => {
  let scenario;
  let dashboard;
  let evaluation;

  test.beforeEach(async ({ page, request }) => {
    scenario = await createProfessorScenario(request, 'PS02', 'plan');
    const login = new LoginPage(page);
    dashboard = new SupervisorDashboardPage(page);
    evaluation = new SupervisorEvaluationPage(page);
    await login.goto();
    await login.login('alevada', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await dashboard.openStudent(scenario.offerName, scenario.student.fullName, 'Aprovar plano');
  });

  test('deve mostrar os dados, abrir o PDF e persistir o parecer e a aprovação', async ({ page }) => {
    await evaluation.expectPlanDetails();
    const download = page.waitForResponse((response) => response.url().endsWith('/work-plan/download'));
    await page.getByRole('button', { name: /Visualizar PDF do Plano/ }).click();
    const pdf = await download;
    expect(pdf.ok()).toBeTruthy();
    expect((await pdf.body()).toString()).toContain('%PDF-1.4');
    await evaluation.approvePlan('Atividades adequadas ao estágio');
    await expect(page.locator('.alert-success')).toHaveText('Plano de trabalho aprovado.');
    await evaluation.expectStatus('Plano aprovado');
    await page.reload();
    await expect(evaluation.status).toHaveText('Plano aprovado');
    await expect(evaluation.planForm).toHaveCount(0);
    const response = await page.request.get(`/api/professor/supervisor/offers/${scenario.offerId}/students/${scenario.student.studentId}/work-plan`);
    await expect(response).toBeOK();
    const plan = await response.json();
    expect(plan.supervisorParecer).toBe('Atividades adequadas ao estágio');
    expect(plan.supervisorApprovedAt).toBeTruthy();
  });

  test('deve exigir o parecer antes de aprovar', async () => {
    await evaluation.planForm.getByRole('button').click();
    expect(await evaluation.planForm.getByRole('textbox').evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    await expect(evaluation.status).toHaveText('Plano enviado');
  });

  test('deve voltar ao painel sem aprovar o plano', async () => {
    await evaluation.planForm.getByRole('textbox').fill('Rascunho não enviado');
    await evaluation.goBack();
    await dashboard.expectStudentStatus(scenario.offerName, scenario.student.fullName, 'Plano enviado');
    await dashboard.openStudent(scenario.offerName, scenario.student.fullName, 'Aprovar plano');
    await expect(evaluation.planForm.getByRole('textbox')).toHaveValue('');
  });
});
