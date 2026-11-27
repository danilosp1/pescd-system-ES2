import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { ResponsibleOffersPage } from '../pages/ResponsibleOffersPage.js';
import { ResponsibleEvaluationPage } from '../pages/ResponsibleEvaluationPage.js';
import { createProfessorScenario } from '../fixtures/professorScenario.js';

test.describe('PR.02 - Análise de documentação pelo responsável', () => {
  let scenario;
  let offers;
  let evaluation;

  test.beforeEach(async ({ page, request }) => {
    scenario = await createProfessorScenario(request, 'PR02', 'documentation');
    const login = new LoginPage(page);
    offers = new ResponsibleOffersPage(page);
    evaluation = new ResponsibleEvaluationPage(page);
    await login.goto();
    await login.login('aendo', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await offers.goto();
    await offers.openStudent(scenario.offerName, scenario.student.fullName, 'Analisar documentação');
  });

  test('deve apresentar a documentação e persistir parecer, frequência e nota', async ({ page }) => {
    const card = page.locator('.card').filter({ hasText: 'Documentação comprobatória' });
    await expect(card).toContainText('Universidade de Teste');
    await expect(card).toContainText('Didática do Ensino Superior');
    await expect(card).toContainText('60h');
    await expect(card.locator('input, textarea, select')).toHaveCount(0);
    await expect(card.getByRole('button', { name: /Visualizar PDF da Documentação/ })).toBeVisible();
    await evaluation.conclude({ parecer: 'Documentação aceita pelo E2E', frequency: 100, grade: 'A' });
    await evaluation.expectConcluded();
    await page.reload();
    await expect(evaluation.status).toHaveText('Concluído pelo responsável');
    const response = await page.request.get(`/api/professor/responsavel/offers/${scenario.offerId}/students/${scenario.student.studentId}/documentation`);
    await expect(response).toBeOK();
    const document = await response.json();
    expect(document.responsavelParecer).toBe('Documentação aceita pelo E2E');
    expect(document.responsavelFrequencia).toBe(100);
    expect(document.responsavelNota).toBe('A');
    expect(document.responsavelApprovedAt).toBeTruthy();
  });

  test('deve exigir nota final antes de concluir a análise', async () => {
    await evaluation.parecerInput.fill('Documentos conferidos');
    await evaluation.frequencyInput.fill('100');
    await evaluation.submitButton.click();
    expect(await evaluation.gradeSelect.evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    await expect(evaluation.status).toHaveText('Documentação enviada');
  });

  test('deve voltar sem aprovar a documentação', async () => {
    await evaluation.parecerInput.fill('Rascunho');
    await evaluation.goBack();
    await expect(offers.offerCard(scenario.offerName)).toContainText('Documentação enviada');
  });
});
