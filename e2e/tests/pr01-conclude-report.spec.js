import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { ResponsibleOffersPage } from '../pages/ResponsibleOffersPage.js';
import { ResponsibleEvaluationPage } from '../pages/ResponsibleEvaluationPage.js';
import { createProfessorScenario } from '../fixtures/professorScenario.js';

test.describe('PR.01 - Conclusão do relatório pelo responsável', () => {
  let scenario;
  let offers;
  let evaluation;

  test.beforeEach(async ({ page, request }) => {
    scenario = await createProfessorScenario(request, 'PR01', 'approvedReport');
    const login = new LoginPage(page);
    offers = new ResponsibleOffersPage(page);
    evaluation = new ResponsibleEvaluationPage(page);
    await login.goto();
    await login.login('aendo', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await offers.goto();
    await offers.openStudent(scenario.offerName, scenario.student.fullName, 'Concluir relatório');
  });

  test('deve exibir a avaliação do supervisor e persistir a avaliação final', async ({ page }) => {
    await expect(page.locator('.supervisor-box')).toContainText('Atividades concluídas conforme o plano');
    await expect(page.locator('.supervisor-box')).toContainText('95%');
    await expect(page.getByRole('button', { name: /PDF do Plano/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /PDF do Relatório/ })).toBeVisible();
    await evaluation.conclude({ parecer: 'Estágio concluído com êxito', frequency: 92, grade: 'B' });
    await evaluation.expectConcluded();
    await page.reload();
    await expect(evaluation.status).toHaveText('Concluído pelo responsável');
    const response = await page.request.get(`/api/professor/responsavel/offers/${scenario.offerId}/students/${scenario.student.studentId}/report`);
    await expect(response).toBeOK();
    const report = await response.json();
    expect(report.responsavelParecer).toBe('Estágio concluído com êxito');
    expect(report.responsavelFrequencia).toBe(92);
    expect(report.responsavelNota).toBe('B');
    expect(report.responsavelApprovedAt).toBeTruthy();
  });

  test('deve exigir os campos e rejeitar frequência negativa', async () => {
    await evaluation.submitButton.click();
    expect(await evaluation.parecerInput.evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    await evaluation.parecerInput.fill('Avaliação final');
    await evaluation.frequencyInput.fill('-1');
    await evaluation.gradeSelect.selectOption('A');
    await evaluation.submitButton.click();
    expect(await evaluation.frequencyInput.evaluate((input) => input.validity.rangeUnderflow)).toBeTruthy();
    await expect(evaluation.status).toHaveText('Relatório aprovado pelo supervisor');
  });

  test('deve voltar sem concluir a avaliação', async () => {
    await evaluation.parecerInput.fill('Rascunho');
    await evaluation.goBack();
    await expect(offers.offerCard(scenario.offerName)).toContainText('Relatório aprovado pelo supervisor');
  });
});
