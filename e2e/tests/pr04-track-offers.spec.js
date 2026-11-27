import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { ResponsibleOffersPage } from '../pages/ResponsibleOffersPage.js';
import { createProfessorScenario } from '../fixtures/professorScenario.js';

test.describe('PR.04 - Acompanhamento das ofertas pelo responsável', () => {
  let scenario;
  let offers;

  test.beforeEach(async ({ page, request }) => {
    scenario = await createProfessorScenario(request, 'PR04', 'mixed');
    const login = new LoginPage(page);
    offers = new ResponsibleOffersPage(page);
    await login.goto();
    await login.login('aendo', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await offers.goto();
  });

  test('deve mostrar os dados da oferta, os alunos e o histórico individual', async ({ page }) => {
    await expect(offers.offerCard(scenario.offerName)).toContainText('2026/2');
    await offers.openOffer(scenario.offerName);
    const information = page.locator('.card').filter({ hasText: 'Informações da oferta' });
    await expect(information).toContainText('André Endo');
    await expect(information).toContainText('Em andamento');
    await expect(offers.rows).toHaveCount(2);
    await offers.expectStudent(scenario.student.fullName, 'Concluído pelo responsável');
    await offers.expectStudent(scenario.secondStudent.fullName, 'Concluído pelo responsável');
    await offers.rows.filter({ hasText: scenario.student.email }).getByRole('link', { name: 'Ver detalhes' }).click();
    await expect(page.getByRole('heading', { name: scenario.student.fullName, exact: true })).toBeVisible();
    await expect(page.locator('.timeline')).toContainText('Plano enviado');
    await expect(page.locator('.timeline')).toContainText('Relatório aprovado pelo supervisor');
    await expect(page.locator('.timeline')).toContainText('Concluído pelo responsável');
  });

  test('deve filtrar por nome, tratar busca vazia e mostrar ausência de resultados', async ({ page }) => {
    await offers.openOffer(scenario.offerName);
    await offers.search(scenario.student.fullName.toUpperCase());
    await expect(offers.rows).toHaveCount(1);
    await offers.expectStudent(scenario.student.fullName, 'Concluído pelo responsável');
    await offers.search('Nome inexistente I3');
    await expect(page.getByText('Nenhum aluno encontrado.', { exact: true })).toBeVisible();
    await offers.search('');
    await expect(offers.rows).toHaveCount(2);
  });

  test('deve impedir acesso de outro professor à oferta', async ({ page }) => {
    const login = new LoginPage(page);
    await login.logout();
    await login.login('mnaldi', '123456');
    await login.expectLoginSuccessful('/professor/supervisor', 'PROFESSOR');
    await page.goto(`/professor/responsavel/ofertas/${scenario.offerId}`);
    await expect(page.locator('.alert-error')).toContainText('Você não é o professor responsável desta oferta.');
    await expect(offers.rows).toHaveCount(0);
  });
});
