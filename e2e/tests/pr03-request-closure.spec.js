import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { ResponsibleOffersPage } from '../pages/ResponsibleOffersPage.js';
import { ResponsibleCloseOfferPage } from '../pages/ResponsibleCloseOfferPage.js';
import { createProfessorScenario } from '../fixtures/professorScenario.js';

test.describe('PR.03 - Solicitação de encerramento pelo responsável', () => {
  let offers;
  let closure;

  test.beforeEach(async ({ page }) => {
    offers = new ResponsibleOffersPage(page);
    closure = new ResponsibleCloseOfferPage(page);
  });

  async function openScenario(page, scenario) {
    const login = new LoginPage(page);
    await login.goto();
    await login.login('aendo', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await offers.goto();
    await offers.openClosure(scenario.offerName);
  }

  test('deve consolidar as duas formas de conclusão e solicitar encerramento', async ({ page, request }) => {
    const scenario = await createProfessorScenario(request, 'PR03', 'mixed');
    await openScenario(page, scenario);
    await closure.expectStatistic('Alunos inscritos', '2');
    await closure.expectStatistic('Frequência média', '97.5%');
    await closure.expectStatistic('Concluídos por documentação', '1');
    await closure.expectStatistic('Concluídos por relatório', '1');
    await closure.expectStudentResult(scenario.student.fullName, 'Relatório', 95, 'A — Excelente');
    await closure.expectStudentResult(scenario.secondStudent.fullName, 'Documentação', 100, 'B — Bom');
    await expect(page.getByText('A: 1', { exact: true })).toBeVisible();
    await expect(page.getByText('B: 1', { exact: true })).toBeVisible();
    await closure.requestClosure('Manter o acompanhamento por etapas');
    await expect(page).toHaveURL(/\/professor\/responsavel$/);
    await expect(offers.offerCard(scenario.offerName)).toContainText('Aguardando encerramento do secretário');
    const response = await page.request.get(`/api/professor/responsavel/offers/${scenario.offerId}`);
    await expect(response).toBeOK();
    const { offer } = await response.json();
    expect(offer.lessonsLearned).toBe('Manter o acompanhamento por etapas');
    expect(offer.closureRequestedAt).toBeTruthy();
    expect(offer.closureRequestedBy.username).toBe('aendo');
  });

  test('deve exigir lições aprendidas e permitir voltar sem encerrar', async ({ page, request }) => {
    const scenario = await createProfessorScenario(request, 'PR03', 'mixed');
    await openScenario(page, scenario);
    await closure.submitButton.click();
    expect(await closure.lessonsInput.evaluate((input) => input.validity.valueMissing)).toBeTruthy();
    await closure.lessonsInput.fill('Rascunho');
    await closure.goBack();
    await expect(offers.offerCard(scenario.offerName)).toContainText('Em andamento');
  });

  test('deve impedir encerramento com aluno pendente', async ({ page, request }) => {
    const scenario = await createProfessorScenario(request, 'PR03', 'plan');
    await openScenario(page, scenario);
    await closure.requestClosure('Tentativa com aluno pendente');
    await expect(closure.errorAlert).toContainText('Todos os alunos devem estar concluídos');
    await closure.goBack();
    await expect(offers.offerCard(scenario.offerName)).toContainText('Em andamento');
  });
});
